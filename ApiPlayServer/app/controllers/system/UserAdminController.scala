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

import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import parsers.{JsonUtils, UserRecordsCSVParser}
import play.api.http.ContentTypes
import play.api.libs.Files
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller, MultipartFormData, Result}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.shared._
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.UserPhysicalDataService

import scala.concurrent.Future


class UserAdminController @Inject()(service: UserAdminService,
                                    userPhysicalData: UserPhysicalDataService,
                                    UsersSupportService: UserPhysicalDataService,
                                    usersSupportService: UsersSupportService,
                                    passwordHasherRegistry: PasswordHasherRegistry,
                                    rab: Intake24RestrictedActionBuilder,
                                    authChecks: UserAuthChecks) extends Controller
  with DatabaseErrorHandler with JsonUtils {

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

    translateDatabaseResult(service.createOrUpdateUsersWithAliases(newUserRecords))
  }

  private def uploadCSV(formData: MultipartFormData[Files.TemporaryFile], surveyId: String, roles: Set[String]): Result = {
    if (formData.files.length != 1)
      BadRequest(toJsonString(ErrorDescription("BadRequest", s"Expected exactly one file attachment, got ${formData.files.length}"))).as(ContentTypes.JSON)
    else {
      UserRecordsCSVParser.parseFile(formData.files(0).ref.file) match {
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

  def createUser() = rab.restrictAccess(authChecks.canCreateUser)(jsonBodyParser[CreateUserRequest]) {
    request =>
      Future {
        val pwInfo = passwordHasherRegistry.current.hash(request.body.password)

        translateDatabaseResult(service.createUserWithPassword(NewUserWithPassword(request.body.userInfo, SecurePassword(pwInfo.password, pwInfo.salt.get, pwInfo.hasher))))
      }
  }

  def patchUserProfile(userId: Long) = rab.restrictAccessWithDatabaseCheck(authChecks.canUpdateProfile(userId))(jsonBodyParser[UserProfileUpdate]) {
    request =>
      Future {
        translateDatabaseResult(service.updateUserProfile(userId, request.body))
      }
  }

  def patchUserPassword(userId: Long) = rab.restrictAccessWithDatabaseCheck(authChecks.canUpdatePassword(userId))(jsonBodyParser[PatchUserPasswordRequest]) {
    request =>
      Future {
        val pwInfo = passwordHasherRegistry.current.hash(request.body.password)
        translateDatabaseResult(service.updateUserPassword(userId, SecurePassword(pwInfo.password, pwInfo.salt.get, pwInfo.hasher)))
      }
  }

  def patchMe() = rab.restrictToAuthenticated(jsonBodyParser[UserProfileUpdate]) {
    request =>
      Future {
        translateDatabaseResult(service.updateUserProfile(request.subject.userId, request.body))
      }
  }

  def deleteUsers() = rab.restrictAccess(authChecks.canCreateUser)(jsonBodyParser[DeleteUsersRequest]) {
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

  def listSurveyStaffUsers(surveyId: String, offset: Int, limit: Int) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(BodyParsers.parse.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.listUsersByRole(Roles.surveyStaff(surveyId), offset, limit))
      }
  }

  def createOrUpdateSurveyStaff(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser[CreateOrUpdateSurveyUsersRequest]) {
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
  def listSurveyRespondentUsers(surveyId: String, offset: Int, limit: Int) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(BodyParsers.parse.empty) {
    _ =>
      Future {
        val result =
          for (users <- service.listUsersByRole(Roles.surveyRespondent(surveyId), offset, limit).right;
               surveyUserNames <- service.getSurveyUserAliases(users.map(_.id), surveyId).right)
            yield
              users.filter(u => surveyUserNames.contains(u.id)).map {
                user =>
                  UserInfoWithSurveyUserName(user.id, surveyUserNames(user.id), user.name, user.email, user.phone, user.emailNotifications, user.smsNotifications, user.roles, user.customFields)
              }

        translateDatabaseResult(result)
      }
  }

  def createOrUpdateSurveyRespondents(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser[CreateOrUpdateSurveyUsersRequest]) {
    request =>
      Future {
        doCreateOrUpdate(surveyId, Set(Roles.surveyRespondent(surveyId)), request.body.users)
      }
  }

  def uploadSurveyRespondentsCSV(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(BodyParsers.parse.multipartFormData) {
    request =>
      Future {
        uploadCSV(request.body, surveyId, Set(Roles.surveyRespondent(surveyId)))
      }
  }

  def deleteSurveyUsers(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser[DeleteSurveyUsersRequest]) {
    request =>
      Future {
        translateDatabaseResult(service.deleteUsersByAlias(request.body.userNames.map(n => SurveyUserAlias(surveyId, n))))
      }
  }

  def createRespondentsWithPhysicalData(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser[CreateRespondentsWithPhysicalDataRequest]) {
    request =>
      Future {
        translateDatabaseResult(usersSupportService.createRespondentsWithPhysicalData(surveyId, request.body.users).right.map {
          userData => CreateRespondentsWithPhysicalDataResponse(userData)
        })
      }
  }

  def giveAccessToSurvey(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser[UserAccessToSurveySeq]) {
    request =>
      Future {
        //        Check that all roles contain surveyId as prefix then perform update for every user
        request.body.containsSurveyId(surveyId) match {
          case true =>
            request.body.users.map(userAccess => service.giveAccessToSurvey(userAccess))
            Ok
          case false => Forbidden
        }
      }
  }

  def withdrawAccessToSurvey(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser[UserAccessToSurveySeq]) {
    request =>
      Future {
        //        Check that all roles contain surveyId as prefix then perform update for every user
        request.body.containsSurveyId(surveyId) match {
          case true =>
            request.body.users.map(userAccess => service.withdrawAccessToSurvey(userAccess))
            Ok
          case false => Forbidden
        }
      }
  }
}
