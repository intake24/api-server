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

package controllers.system.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import javax.inject.Inject
import org.slf4j.LoggerFactory
import parsers.JsonUtils
import play.api.Configuration
import play.api.mvc._
import uk.ac.ncl.openlab.intake24.api.data.{ErrorDescription, NewUserProfile}
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

case class GeneratedCredentials(userName: String, password: String)

case class CreateUserParameters(userId: String, redirect: String)

case class CreateUserResponse(userId: Long, redirect: String, authToken: String)

class GeneratedUsersController @Inject()(userAdminService: UserAdminService,
                                         surveyAdminService: SurveyAdminService,
                                         passwordHasherRegistry: PasswordHasherRegistry,
                                         playBodyParsers: PlayBodyParsers,
                                         configuration: Configuration,
                                         val controllerComponents: ControllerComponents,
                                         implicit val executionContext: ExecutionContext) extends BaseController
  with DatabaseErrorHandler with JsonUtils {

  val logger = LoggerFactory.getLogger(classOf[GeneratedUsersController])

  private def generateCredentials(surveyId: String, counter: Int): GeneratedCredentials = {
    val userName = surveyId + Integer.toString(counter)
    val password = Random.alphanumeric.take(5).mkString.toLowerCase

    GeneratedCredentials(userName, password)
  }

  private def mkNewUserRecord(surveyId: String, credentials: GeneratedCredentials): NewUserWithAlias = {
    val pwInfo = passwordHasherRegistry.current.hash(credentials.password)

    NewUserWithAlias(
      SurveyUserAlias(surveyId, credentials.userName),
      NewUserProfile(None, None, None, Set(Roles.surveyRespondent(surveyId)), Map()),
      SecurePassword(pwInfo.password, pwInfo.salt.get, pwInfo.hasher)
    )
  }

  // TODO: captcha to prevent new user spam
  def generateUser(surveyId: String) = Action.async(playBodyParsers.empty) {
    _ =>
      Future {
        surveyAdminService.getSurveyParameters(surveyId) match {
          case Right(surveyParams) =>
            if (!surveyParams.allowGeneratedUsers || surveyParams.generateUserKey.isDefined)
              Forbidden(toJsonString(ErrorDescription("GenUsersNotAllowed", "Generated users are not allowed for this survey")))
            else {
              val result = userAdminService.nextGeneratedUserId(surveyId).right.flatMap {
                counter =>
                  val credentials = generateCredentials(surveyId, counter)
                  val newUserRecord = mkNewUserRecord(surveyId, credentials)

                  userAdminService.createOrUpdateUsersWithAliases(Seq(newUserRecord)).right.map(_ => credentials)
              }

              translateDatabaseResult(result)
            }
          case Left(error) =>
            translateDatabaseError(error)
        }
      }
  }

  def decodeCreateUserParameters(key: String, token: String): Either[String, CreateUserParameters] = {
    val verifier = JWT.require(Algorithm.HMAC256(key)).build()

    try {
      val decodedJwt = verifier.verify(token)

      val redirect = decodedJwt.getClaim("redirect")
      val user = decodedJwt.getClaim("user")

      if (redirect.isNull())
        Left("Required claim missing: redirect")
      else if (user.isNull())
        Left("Required claim missing: user")
      else
        Right(CreateUserParameters(user.asString(), redirect.asString()))
    } catch {
      case e: JWTVerificationException =>
        logger.debug(s"JWT decode failed: ${e.getClass.getSimpleName}: ${e.getMessage}")
        Left("Invalid JWT")
    }
  }

  def createUser(surveyId: String, params: String) = Action.async(playBodyParsers.empty) {
    _ =>
      Future {
        surveyAdminService.getSurveyParameters(surveyId) match {
          case Right(surveyParams) =>
            if (!surveyParams.allowGeneratedUsers)
              Forbidden(toJsonString(ErrorDescription("GenUsersNotAllowed", "Generated users are not allowed for this survey")))
            else surveyParams.generateUserKey match {
              case Some(key) =>
                decodeCreateUserParameters(key, params) match {
                  case Right(CreateUserParameters(userId, redirect)) =>
                    val newUserRecord = mkNewUserRecord(surveyId, GeneratedCredentials(userId, Random.alphanumeric.take(16).mkString))

                    translateDatabaseResult(userAdminService.createOrUpdateUsersWithAliases(Seq(newUserRecord)).right.map(newUsers =>
                      CreateUserResponse(newUsers.head.userId, redirect, newUsers.head.urlAuthToken)))

                  case Left(message) =>
                    BadRequest(toJsonString(ErrorDescription("TokenDecodeFailed", message)))
                }

              case None =>
                Forbidden(toJsonString(ErrorDescription("GenUsersParamsNotAllowed", "Generated users with parameters are not allowed for this survey")))
            }
          case Left(error) =>
            translateDatabaseError(error)
        }
      }
  }
}
