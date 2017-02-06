/*
This file is part of Intake24.

Copyright 2015, 2016, 2017 Newcastle University.

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

import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.exceptions.{IdentityNotFoundException, InvalidPasswordException}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import parsers.UpickleUtil
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import security.{DatabaseAccessException, DatabaseFormatException, DeadboltActionsAdapter, Intake24ApiEnv}
import uk.ac.ncl.openlab.intake24.api.shared.{ErrorDescription, RefreshResult, SigninResult, Credentials => Intake24Credentials}
import upickle.default._

import scala.concurrent.Future

class SigninController @Inject()(@Named("refresh") refreshEnv: Environment[Intake24ApiEnv], @Named("access") accessEnv: Environment[Intake24ApiEnv],
                                 credentialsProvider: CredentialsProvider, deadbolt: DeadboltActionsAdapter)
  extends Controller with UpickleUtil {

  def signin = Action.async(upickleBodyParser[Intake24Credentials]) {
    implicit request =>

      val credentials = request.body

      val authResult = credentialsProvider.authenticate(Credentials(credentials.username + "#" + credentials.survey_id, credentials.password))

      authResult.flatMap {
        loginInfo =>
          refreshEnv.identityService.retrieve(loginInfo).flatMap {
            case Some(user) => refreshEnv.authenticatorService.create(loginInfo).flatMap {
              authenticator =>

                val customClaims = Json.obj("i24t" -> "refresh")

                refreshEnv.authenticatorService.init(authenticator.copy(customClaims = Some(customClaims))).map { token =>
                  Ok(write(SigninResult(token))).as(ContentTypes.JSON)
                }
            }
            case None =>
              Future.successful(Unauthorized)
          }
      }.recover {
        case e: IdentityNotFoundException => Unauthorized
        case e: InvalidPasswordException => Unauthorized
        case e: DatabaseFormatException => InternalServerError(write(ErrorDescription("DatabaseFormatException", e.toString())))
        case e: DatabaseAccessException => InternalServerError(write(ErrorDescription("DatabaseAccessException", e.toString())))
      }
  }

  def refresh = deadbolt.restrictRefresh {
    implicit request =>
      val loginInfo = LoginInfo("credentials", request.subject.get.identifier)

      refreshEnv.identityService.retrieve(loginInfo).flatMap {
        case Some(user) => accessEnv.authenticatorService.create(loginInfo).flatMap {
          authenticator =>
            val customClaims = Json.obj("i24t" -> "access", "i24r" -> user.securityInfo.roles, "i24p" -> user.securityInfo.permissions)

            refreshEnv.authenticatorService.init(authenticator.copy(customClaims = Some(customClaims))).map {
              token => Ok(write(RefreshResult("NotImplemented", token))).as(ContentTypes.JSON)
            }
        }
        case None =>
          Future.successful(Unauthorized)
      }.recover {
        case e: IdentityNotFoundException => Unauthorized
        case e: InvalidPasswordException => Unauthorized
        case e: DatabaseFormatException => InternalServerError(write(ErrorDescription("DatabaseFormatException", e.toString())))
        case e: DatabaseAccessException => InternalServerError(write(ErrorDescription("DatabaseAccessException", e.toString())))
      }
  }

}