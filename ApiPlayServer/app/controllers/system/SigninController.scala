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

import akka.actor.ActorSystem
import com.google.inject.name.Named
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.exceptions.{IdentityNotFoundException, InvalidPasswordException}
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import models.RefreshSubject
import parsers.JsonUtils
import play.api.Logger
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, RequestHeader, Result}
import security._
import uk.ac.ncl.openlab.intake24.api.shared._
import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SigninAttempt, SigninLogService, SurveyUserAlias}

import scala.concurrent.Future
import scala.concurrent.duration._

class SigninController @Inject()(@Named("refresh") refreshEnv: Environment[Intake24ApiEnv], @Named("access") accessEnv: Environment[Intake24ApiEnv],
                                 emailProvider: EmailProvider, surveyAliasProvider: SurveyAliasProvider, urlTokenProvider: URLTokenProvider,
                                 signinLogService: SigninLogService, actorSystem: ActorSystem, deadbolt: DeadboltActionsAdapter)
  extends Controller with JsonUtils with DatabaseErrorHandler {


  def logAttemptAsync(event: SigninAttempt) = {
    actorSystem.scheduler.scheduleOnce(0 seconds) {
      signinLogService.logSigninAttempt(event) match {
        case Right(()) => ()
        case Left(UnexpectedDatabaseError(e)) => Logger.error("Failed to log sign in attempt", e)
      }
    }
  }

  def getRemoteAddress(request: RequestHeader) = request.headers.get("X-Real-IP").getOrElse(request.remoteAddress)

  def handleAuthResult(providerID: String, providerKey: String, authResult: Future[LoginInfo])(implicit request: RequestHeader): Future[Result] = {

    def logException(e: Throwable) = logAttemptAsync(SigninAttempt(getRemoteAddress(request), providerID, providerKey, false, None, Some(e.getClass.getSimpleName + ": " + e.getMessage)))

    authResult.flatMap {
      loginInfo =>
        refreshEnv.identityService.retrieve(loginInfo).flatMap {
          case Some(user) =>

            logAttemptAsync(SigninAttempt(getRemoteAddress(request), providerID, providerKey, true, Some(user.userInfo.id), None))

            refreshEnv.authenticatorService.create(loginInfo).flatMap {
              authenticator =>

                val customClaims = Json.obj("type" -> "refresh", "userId" -> user.userInfo.id)

                refreshEnv.authenticatorService.init(authenticator.copy(customClaims = Some(customClaims))).map {
                  token =>
                    Ok(toJsonString(SigninResult(token))).as(ContentTypes.JSON)
                }
            }
          case None =>
            logAttemptAsync(SigninAttempt(getRemoteAddress(request), loginInfo.providerID, loginInfo.providerKey, false, None, Some("Authentication was successful, but identity service could not find user")))
            Future.successful(Unauthorized)
        }
    }.recover {
      case e: IdentityNotFoundException => {
        logException(e)
        Unauthorized
      }
      case e: InvalidPasswordException => {
        logException(e)
        Unauthorized
      }
      case e: DatabaseFormatException => {
        logException(e)
        InternalServerError(toJsonString(ErrorDescription("DatabaseFormatException", e.toString())))
      }
      case e: DatabaseAccessException => {
        logException(e)
        InternalServerError(toJsonString(ErrorDescription("DatabaseAccessException", e.toString())))
      }
    }
  }

  def signinWithAlias = Action.async(jsonBodyParser[SurveyAliasCredentials]) {
    implicit request =>

      val credentials = request.body

      val providerKey = SurveyAliasUtils.toString(SurveyUserAlias(credentials.surveyId, credentials.userName))

      val authResult = surveyAliasProvider.authenticate(Credentials(providerKey, credentials.password))

      handleAuthResult(SurveyAliasProvider.ID, providerKey, authResult)
  }

  def signinWithEmail = Action.async(jsonBodyParser[EmailCredentials]) {
    implicit request =>

      val credentials = request.body

      val authResult = emailProvider.authenticate(Credentials(credentials.email, credentials.password))

      handleAuthResult(EmailProvider.ID, credentials.email, authResult)
  }

  def signinWithToken(authToken: String) = Action.async {
    implicit request =>
      val authResult = urlTokenProvider.authenticate(authToken)

      handleAuthResult(URLTokenProvider.ID, authToken, authResult)
  }

  def refresh = deadbolt.restrictRefresh {
    implicit request =>

      request.subject match {
        case Some(RefreshSubject(identifier, userId, jwt)) =>
          refreshEnv.identityService.retrieve(jwt.loginInfo).flatMap {
            case Some(user) => accessEnv.authenticatorService.create(jwt.loginInfo).flatMap {
              accessToken =>
                val customClaims = Json.obj("type" -> "access", "userId" -> user.userInfo.id, "roles" -> user.userInfo.roles.toList)
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