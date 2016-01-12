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

package controllers

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.LoginEvent
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.exceptions.InvalidPasswordException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import javax.inject.Inject
import models.User
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import security.AuthenticationException
import security.DatabaseAccessException
import security.DatabaseFormatException
import upickle.Invalid
import upickle.default.macroR
import upickle.default.macroW
import upickle.default.read
import upickle.default.write
import uk.ac.ncl.openlab.intake24.api.Intake24Credentials
import play.api.libs.iteratee.Enumerator
import play.api.http.Writeable

class Auth @Inject() (
  val messagesApi: MessagesApi,
  val env: Environment[User, JWTAuthenticator],
  val credentialsProvider: CredentialsProvider)

  extends Silhouette[User, JWTAuthenticator] {

  def signin = Action.async(parse.tolerantText) { implicit request =>

    try {
      val credentials = read[Intake24Credentials](request.body)

      val authResult = credentialsProvider.authenticate(Credentials(credentials.username + "#" + credentials.survey_id, credentials.password))

      authResult.flatMap { loginInfo =>
        env.identityService.retrieve(loginInfo).flatMap {
          case Some(user) => env.authenticatorService.create(loginInfo).flatMap {
            authenticator =>
              
              val customClaims = Json.obj( "i24r" -> user.securityInfo.roles, "i24p" -> user.securityInfo.permissions)
              
              env.eventBus.publish(LoginEvent(user, request, request2Messages))
              env.authenticatorService.init(authenticator.copy(customClaims = Some(customClaims))).map { token =>
                Ok(Json.obj("token" -> token))
              }
          }
          case None =>
            Future.failed(new AuthenticationException("Couldn't find user"))
        }
      }.recoverWith(exceptionHandler).recover {
        case e: IdentityNotFoundException => Unauthorized
        case e: InvalidPasswordException => Unauthorized
        case e: DatabaseFormatException => InternalServerError(Json.obj("error" -> "databaseFormatException", "debugMessage" -> e.toString()))
        case e: DatabaseAccessException => InternalServerError(Json.obj("error" -> "databaseAccessException", "debugMessage" -> e.toString()))
      }
    } catch {
      case Invalid.Data(_, msg) => Future.successful(BadRequest(Json.obj("error" -> "jsonException", "debugMessage" -> msg)))
      case Invalid.Json(msg, input) => Future.successful(BadRequest(Json.obj("error" -> "jsonException", "debugMessage" -> msg)))
    }
  }

  def test = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => {
        Logger.debug("Some user")
        Future.successful(Ok(user.toString()))
      }
      case None => {
        Logger.debug("None")
        Future.successful(Ok(":("))
      }
    }
  }
}