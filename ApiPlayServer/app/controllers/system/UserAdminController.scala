/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package controllers.system

import java.io.StringWriter
import java.security.SecureRandom
import java.util.Base64

import akka.stream.scaladsl.{Source, StreamConverters}
import javax.inject.Inject
import au.com.bytecode.opencsv.CSVWriter
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import org.reactivestreams.Publisher
import parsers.{JsonBodyParser, JsonUtils, UserRecordsCSVParser}
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.http.ContentTypes
import play.api.libs.Files
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
import security.Intake24RestrictedActionBuilder
import security.captcha.AsyncCaptchaService
import uk.ac.ncl.openlab.intake24.api.data._
import uk.ac.ncl.openlab.intake24.errors.{ErrorUtils, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.UserPhysicalDataService
import uk.ac.ncl.openlab.intake24.shorturls.{ShortUrlsHttpClient, ShortUrlsRequest}
import views.html.PasswordResetLink

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


case class CreateOrUpdateResponse(users: Seq[NewRespondentIds])

class UserAdminController @Inject()(service: UserAdminService,
                                    userPhysicalData: UserPhysicalDataService,
                                    UsersSupportService: UserPhysicalDataService,
                                    usersSupportService: UsersSupportService,
                                    passwordHasherRegistry: PasswordHasherRegistry,
                                    rab: Intake24RestrictedActionBuilder,
                                    authChecks: UserAuthChecks,
                                    playBodyParsers: PlayBodyParsers,
                                    jsonBodyParser: JsonBodyParser,
                                    mailerClient: MailerClient,
                                    syncCacheApi: SyncCacheApi,
                                    captchaService: AsyncCaptchaService,
                                    shortUrlsClient: ShortUrlsHttpClient,
                                    configuration: Configuration,
                                    val controllerComponents: ControllerComponents,
                                    implicit val executionContext: ExecutionContext) extends BaseController
  with DatabaseErrorHandler with JsonUtils {


  private lazy val random = new SecureRandom()
  private lazy val base64Encoder = Base64.getUrlEncoder()

  private lazy val adminFrontendUrl = {
    val setting = configuration.get[String]("intake24.adminFrontendUrl")

    if (!setting.endsWith("/"))
      setting + "/"
    else
      setting
  }

  private lazy val surveyFrontendUrl = {
    val setting = configuration.get[String]("intake24.surveyFrontendUrl")

    if (!setting.endsWith("/"))
      setting + "/"
    else
      setting
  }


  private def doCreateOrUpdate(surveyId: String, roles: Set[String], userRecords: Seq[NewRespondent]): Result = {
    val hasher = passwordHasherRegistry.current

    val newUserRecords = userRecords.map {
      record =>
        val passwordInfo = hasher.hash(record.password)

        NewUserWithAlias(
          SurveyUserAlias(surveyId, record.userName),
          NewUserProfile(record.name, record.email, record.phone, roles, record.customFields),
          SecurePassword(passwordInfo.password, passwordInfo.salt.get, passwordInfo.hasher))
    }

    translateDatabaseResult(service.createOrUpdateUsersWithAliases(newUserRecords).map(CreateOrUpdateResponse(_)))
  }

  private def uploadCSV(formData: MultipartFormData[Files.TemporaryFile], surveyId: String, roles: Set[String]): Result = {
    if (formData.files.length != 1)
      BadRequest(toJsonString(ErrorDescription("BadRequest", s"Expected exactly one file attachment, got ${formData.files.length}"))).as(ContentTypes.JSON)
    else {
      UserRecordsCSVParser.parseFile(formData.files(0).ref.path.toFile) match {
        case Right(csvRecords) =>
          doCreateOrUpdate(surveyId, roles, csvRecords)
        case Left(error) =>
          BadRequest(toJsonString(ErrorDescription("InvalidCSV", error)))
      }
    }
  }

  def findUsers(query: String, limit: Int) = rab.restrictAccess(authChecks.canListUsers) {
    _ =>
      Future {
        translateDatabaseResult(service.findUsers(query, Math.min(Math.max(limit, 0), 100)))
      }
  }

  def createUser() = rab.restrictAccess(authChecks.canCreateUser)(jsonBodyParser.parse[CreateUserRequest]) {
    request =>
      Future {
        val pwInfo = passwordHasherRegistry.current.hash(request.body.password)

        translateDatabaseResult(service.createUserWithPassword(NewUserWithPassword(request.body.userInfo, SecurePassword(pwInfo.password, pwInfo.salt.get, pwInfo.hasher))))
      }
  }

  def patchUserProfile(userId: Long) = rab.restrictAccessWithDatabaseCheck(authChecks.canUpdateProfile(userId))(jsonBodyParser.parse[UserProfileUpdate]) {
    request =>
      Future {
        translateDatabaseResult(service.updateUserProfile(userId, request.body))
      }
  }

  def patchUserPassword(userId: Long) = rab.restrictAccessWithDatabaseCheck(authChecks.canUpdatePassword(userId))(jsonBodyParser.parse[PatchUserPasswordRequest]) {
    request =>
      Future {
        val pwInfo = passwordHasherRegistry.current.hash(request.body.password)
        translateDatabaseResult(service.updateUserPassword(userId, SecurePassword(pwInfo.password, pwInfo.salt.get, pwInfo.hasher)))
      }
  }

  def patchMe() = rab.restrictToAuthenticated(jsonBodyParser.parse[UserProfileUpdate]) {
    request =>
      Future {
        translateDatabaseResult(service.updateUserProfile(request.subject.userId, request.body))
      }
  }

  def deleteUsers() = rab.restrictAccess(authChecks.canCreateUser)(jsonBodyParser.parse[DeleteUsersRequest]) {
    request =>
      Future {
        translateDatabaseResult(service.deleteUsersById(request.body.userIds))
      }
  }

  def deleteUser(userId: Long) = rab.restrictAccessWithDatabaseCheck(authChecks.canDeleteUser(userId)) {
    _ =>
      Future {
        translateDatabaseResult(service.deleteUsersById(Seq(userId)))
      }
  }

  def listSurveyStaffUsers(surveyId: String, offset: Int, limit: Int) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(playBodyParsers.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.listUsersByRole(Roles.surveyStaff(surveyId), offset, limit))
      }
  }

  def createOrUpdateSurveyStaff(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser.parse[CreateOrUpdateSurveyUsersRequest]) {
    request =>
      Future {
        doCreateOrUpdate(surveyId, Set(Roles.surveyStaff(surveyId)), request.body.users)
      }
  }

  /**
    * Only users that have a user name in this survey will be returned.
    *
    * If someone has a respondent role but does not have a user alias for this survey they will be filtered out.
    *
    * This is because client-side user presentation currently does not make sense without a user name.
    */
  def listSurveyRespondentUsers(surveyId: String, offset: Int, limit: Int) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(playBodyParsers.empty) {
    _ =>
      Future {
        val result =
          for (users <- service.listUsersByRole(Roles.surveyRespondent(surveyId), offset, limit).right;
               aliases <- service.getSurveyUserAliases(users.map(_.id), surveyId).right)
            yield
              users.filter(u => aliases.contains(u.id)).map {
                user =>

                  val authUrl = surveyFrontendUrl + s"surveys/$surveyId?auth=${aliases(user.id).urlAuthToken}"

                  UserInfoWithSurveyUserName(user.id, aliases(user.id).userName, authUrl, user.name, user.email, user.phone, user.emailNotifications, user.smsNotifications, user.roles, user.customFields)
              }

        translateDatabaseResult(result)
      }
  }

  private case class RespondentWithAuthUrl(id: Long, userName: String, authUrl: String)

  private case class RespondentWithAuthAndShortUrls(id: Long, userName: String, authUrl: String, shortUrl: String)

  private def getRespondentsWithAuthUrls(surveyId: String, offset: Int, limit: Int): Future[Seq[RespondentWithAuthUrl]] = {
    Future {
      for (users <- service.listUsersByRole(Roles.surveyRespondent(surveyId), offset, limit);
           aliases <- service.getSurveyUserAliases(users.map(_.id), surveyId)) yield {

        users.filter(u => aliases.contains(u.id)).map {
          user =>
            RespondentWithAuthUrl(user.id, aliases(user.id).userName, surveyFrontendUrl + s"surveys/$surveyId?auth=${aliases(user.id).urlAuthToken}")
        }
      }
    }.flatMap {
      result => ErrorUtils.asFuture(result)
    }
  }


  private def appendShortUrls(respondents: Seq[RespondentWithAuthUrl]): Try[Seq[RespondentWithAuthAndShortUrls]] = {

    shortUrlsClient.getShortUrls(ShortUrlsRequest(respondents.map(_.authUrl))).attempt.unsafeRunSync.map {
      response =>
        respondents.zip(response.shortUrls).map {
          case (r, shortUrl) => RespondentWithAuthAndShortUrls(r.id, r.userName, r.authUrl, shortUrl)
        }
    } match {
      case Left(error) => Failure(error)
      case Right(value) => Success(value)
    }
  }

  def getRespondentAuthenticationUrlsAsJson(surveyId: String, offset: Int, limit: Int) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(playBodyParsers.empty) {
    _ =>
      for (respondents <- getRespondentsWithAuthUrls(surveyId, offset, Math.max(0, Math.min(limit, 1000)));
           result <- Future.fromTry(appendShortUrls(respondents))) yield {
        Ok(toJsonString(result)).as(ContentTypes.JSON)
      }
  }

  private def getRespondentAuthenticationUrlsAsCsvStream(surveyId: String): Iterator[String] = new Iterator[String] {

    val limit = 1000
    var offset = 0

    private def toCsvChunk(rows: Seq[Array[String]]) = {
      val stringWriter = new StringWriter()
      val csvWriter = new CSVWriter(stringWriter)
      rows.foreach(csvWriter.writeNext(_))
      csvWriter.close()
      stringWriter.close()
      stringWriter.toString()
    }

    var cachedNext: Option[String] = Some(toCsvChunk(Seq(Array("Intake24 user ID", "Survey user ID", "Authentication URL", "Short authentication URL"))))

    override def hasNext: Boolean = cachedNext match {
      case Some(_) =>
        true

      case None =>
        val dbResult = for (users <- service.listUsersByRole(Roles.surveyRespondent(surveyId), offset, limit);
                            aliases <- service.getSurveyUserAliases(users.map(_.id), surveyId)) yield {

          users.filter(u => aliases.contains(u.id)).map {
            user =>
              RespondentWithAuthUrl(user.id, aliases(user.id).userName, surveyFrontendUrl + s"surveys/$surveyId?auth=${aliases(user.id).urlAuthToken}")
          }
        }


        dbResult match {
          case Right(rows) if rows.nonEmpty =>

            val withShortUrls = appendShortUrls(rows).get  // boom :-(

            cachedNext = Some(toCsvChunk(withShortUrls.map(row => Array(row.id.toString, row.userName, row.authUrl, row.shortUrl))))
            offset += rows.length
            true
          case Right(_) =>
            cachedNext = None
            false
          case Left(error) =>
            throw error.exception
        }
    }

    override def next(): String = cachedNext match {
      case Some(e) =>
        cachedNext = None
        e
      case None =>
        if (hasNext)
          next()
        else
          throw new NoSuchElementException
    }
  }

  def getRespondentAuthenticationUrlsAsCsv(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(playBodyParsers.empty) {
    _ =>
      Future {
        val source = Source.fromIterator {
          () => getRespondentAuthenticationUrlsAsCsvStream(surveyId)
        }

        Ok.chunked(source)
          .as("text/csv")
          .withHeaders("Content-Disposition" -> "attachment; filename=test.csv")
      }
  }

  def createOrUpdateSurveyRespondents(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser.parse[CreateOrUpdateSurveyUsersRequest]) {
    request =>
      Future {
        doCreateOrUpdate(surveyId, Set(Roles.surveyRespondent(surveyId)), request.body.users)
      }
  }

  def uploadSurveyRespondentsCSV(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(playBodyParsers.multipartFormData) {
    request =>
      Future {
        uploadCSV(request.body, surveyId, Set(Roles.surveyRespondent(surveyId)))
      }
  }

  def deleteSurveyUsers(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser.parse[DeleteSurveyUsersRequest]) {
    request =>
      Future {
        translateDatabaseResult(service.deleteUsersByAlias(request.body.userNames.map(n => SurveyUserAlias(surveyId, n))))
      }
  }

  def createRespondentsWithPhysicalData(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser.parse[CreateRespondentsWithPhysicalDataRequest]) {
    request =>
      Future {
        translateDatabaseResult(usersSupportService.createRespondentsWithPhysicalData(surveyId, request.body.users).right.map {
          userData => CreateRespondentsWithPhysicalDataResponse(userData)
        })
      }
  }

  def giveAccessToSurvey(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser.parse[UserAccessToSurveySeq]) {
    request =>
      Future {

        //        Check that all roles contain surveyId as prefix then perform update for every user
        request.body.containsSurveyId(surveyId) match {
          case true => translateDatabaseResult(ErrorUtils.sequence(request.body.users.map(userAccess => service.giveAccessToSurvey(userAccess))))
          case false => Forbidden
        }
      }
  }

  def withdrawAccessToSurvey(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser.parse[UserAccessToSurveySeq]) {
    request =>
      Future {
        //        Check that all roles contain surveyId as prefix then perform update for every user
        request.body.containsSurveyId(surveyId) match {
          case true => translateDatabaseResult(ErrorUtils.sequence(request.body.users.map(userAccess => service.withdrawAccessToSurvey(userAccess))))
          case false => Forbidden
        }
      }
  }

  private def passwordResetTokenCacheKey(token: String) = s"UserAdminController.passwordReset.$token"

  private def doPasswordReset(email: String): Result = {
    val bytes = new Array[Byte](33) // 256 bit + extra char to prevent base64 padding
    random.nextBytes(bytes)

    val token = base64Encoder.encodeToString(bytes)

    service.getUserByEmail(email) match {
      case Right(profile) =>
        syncCacheApi.set(passwordResetTokenCacheKey(token), profile.id, 4.hours)

        val passwordResetUrl = adminFrontendUrl + "password-reset/#?token=" + token

        profile.email match {
          case Some(address) =>
            val body = PasswordResetLink(profile.name, passwordResetUrl, 4)
            val email = Email("Intake24 password reset link", "Intake24 <support@intake24.co.uk>", Seq(address), None, Some(body.toString()))
            mailerClient.send(email)
            Ok
          case None => Ok
        }

      case Left(RecordNotFound(e)) => Ok
      case Left(error) => translateDatabaseError(error)
    }
  }

  def passwordResetRequest() = Action.async(jsonBodyParser.parse[PasswordResetRequest]) {
    request =>

      val recaptchaEnabled = configuration.get[Boolean]("intake24.recaptcha.enabled")

      if (recaptchaEnabled) {
        captchaService.verify(request.body.recaptchaResponse, request.remoteAddress).flatMap {
          case Some(true) =>
            Future {
              doPasswordReset(request.body.email)
            }

          case Some(false) => Future.successful(Forbidden)

          case None => Future.successful(InternalServerError)
        }
      }
      else {
        Future {
          doPasswordReset(request.body.email)
        }
      }
  }

  def resetPassword() = Action.async(jsonBodyParser.parse[PasswordResetConfirmation]) {
    request =>
      Future {
        syncCacheApi.get[Long](passwordResetTokenCacheKey(request.body.token)) match {
          case Some(userId) =>

            if (request.body.newPassword.length < 8)
              BadRequest(toJsonString(ErrorDescription("InvalidPassword", "Password must be at least 8 characters long")))
            else {
              syncCacheApi.remove(passwordResetTokenCacheKey(request.body.token))

              val pwInfo = passwordHasherRegistry.current.hash(request.body.newPassword)
              translateDatabaseResult(service.updateUserPassword(userId, SecurePassword(pwInfo.password, pwInfo.salt.get, pwInfo.hasher)))
            }
          case None =>
            Forbidden(toJsonString(ErrorDescription("InvalidToken", "Password reset token is invalid or has expired")))
        }
      }
  }
}
