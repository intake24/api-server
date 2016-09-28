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

import scala.annotation.implicitNotFound
import scala.concurrent.Future

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.LoginEvent
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.exceptions.InvalidPasswordException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider

import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import security.DatabaseAccessException
import security.DatabaseFormatException
import security.Intake24ApiEnv
import security.Intake24Credentials
import upickle.default._
import play.api.http.ContentTypes

class Auth @Inject() (silEnv: Environment[Intake24ApiEnv], credentialsProvider: CredentialsProvider)
    extends Controller
    with PickleErrorHandler {

  private case class AuthSuccess(token: String)

  def signin = Action.async(parse.tolerantText) {
    implicit request =>

      tryWithPickleAsync {
        val credentials = read[Intake24Credentials](request.body)

        val authResult = credentialsProvider.authenticate(Credentials(credentials.username + "#" + credentials.survey_id, credentials.password))

        authResult.flatMap {
          loginInfo =>
            silEnv.identityService.retrieve(loginInfo).flatMap {
              case Some(user) => silEnv.authenticatorService.create(loginInfo).flatMap {
                authenticator =>

                  val customClaims = Json.obj("i24r" -> user.securityInfo.roles, "i24p" -> user.securityInfo.permissions)

                  silEnv.eventBus.publish(LoginEvent(user, request))
                  silEnv.authenticatorService.init(authenticator.copy(customClaims = Some(customClaims))).map { token =>
                    Ok(write(AuthSuccess(token))).as(ContentTypes.JSON)
                  }
              }
              case None =>
                Future.successful(Unauthorized)
            }
        }.recover {
          case e: IdentityNotFoundException => Unauthorized
          case e: InvalidPasswordException => Unauthorized
          case e: DatabaseFormatException => InternalServerError(Json.obj("error" -> "databaseFormatException", "debugMessage" -> e.toString()))
          case e: DatabaseAccessException => InternalServerError(Json.obj("error" -> "databaseAccessException", "debugMessage" -> e.toString()))
        }
      }
  }

}