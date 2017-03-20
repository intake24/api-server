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
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import models.RefreshSubject
import parsers.JsonUtils
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import security._
import uk.ac.ncl.openlab.intake24.api.shared.{ErrorDescription, RefreshResult, SigninResult, Credentials => Intake24Credentials}

import scala.concurrent.Future

class SigninController @Inject()(@Named("refresh") refreshEnv: Environment[Intake24ApiEnv], @Named("access") accessEnv: Environment[Intake24ApiEnv],
                                 credentialsProvider: CredentialsProvider, deadbolt: DeadboltActionsAdapter)
  extends Controller with JsonUtils with DatabaseErrorHandler {

  def signin = Action.async(jsonBodyParser[Intake24Credentials]) {
    implicit request =>

      val credentials = request.body

      val authResult = credentialsProvider.authenticate(Credentials(Intake24UserKey(credentials.survey_id, credentials.username).toString, credentials.password))

      authResult.flatMap {
        loginInfo =>
          refreshEnv.identityService.retrieve(loginInfo).flatMap {
            case Some(user) => refreshEnv.authenticatorService.create(loginInfo).flatMap {
              authenticator =>

                val customClaims = Json.obj("i24t" -> "refresh")

                refreshEnv.authenticatorService.init(authenticator.copy(customClaims = Some(customClaims))).map { token =>
                  Ok(toJsonString(SigninResult(token))).as(ContentTypes.JSON)
                }
            }
            case None =>
              Future.successful(Unauthorized)
          }
      }.recover {
        case e: IdentityNotFoundException => Unauthorized
        case e: InvalidPasswordException => Unauthorized
        case e: DatabaseFormatException => InternalServerError(toJsonString(ErrorDescription("DatabaseFormatException", e.toString())))
        case e: DatabaseAccessException => InternalServerError(toJsonString(ErrorDescription("DatabaseAccessException", e.toString())))
      }
  }

  def refresh = deadbolt.restrictRefresh {
    implicit request =>

      request.subject match {
        case Some(RefreshSubject(identifier, refreshToken)) =>
          val loginInfo = LoginInfo("credentials", identifier)

          refreshEnv.identityService.retrieve(loginInfo).flatMap {
            case Some(user) => accessEnv.authenticatorService.create(loginInfo).flatMap {
              accessToken =>
                val customClaims = Json.obj("i24t" -> "access", "i24r" -> user.securityInfo.roles, "i24p" -> user.securityInfo.permissions)
                /*
                // This code is for idle expiration of refresh tokens, disabled for simplicity

                val updatedRefreshToken = refreshEnv.authenticatorService.touch(refreshToken) match {
                  case Left(x) => x
                  case Right(x) => x
                }

                for (accessTokenValue <- accessEnv.authenticatorService.init(accessToken.copy(customClaims = Some(customClaims)))));
                     refreshTokenValue <- refreshEnv.authenticatorService.init(updatedRefreshToken))
                 */

                accessEnv.authenticatorService.init(accessToken.copy(customClaims = Some(customClaims))).map {
                  serialisedAccessToken => (Ok(toJsonString(RefreshResult(serialisedAccessToken))).as(ContentTypes.JSON))
                }

            }
            case None =>
              Future.successful(Unauthorized)
          }.recover {
            case e: IdentityNotFoundException => Unauthorized
            case e: InvalidPasswordException => Unauthorized
            case e: DatabaseFormatException => InternalServerError(toJsonString(ErrorDescription("DatabaseFormatException", e.toString())))
            case e: DatabaseAccessException => InternalServerError(toJsonString(ErrorDescription("DatabaseAccessException", e.toString())))
          }

        case _ => Future.successful(Unauthorized.withHeaders(("WWW-Authenticate", "X-Auth-Token")))
      }
  }
}