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
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.exceptions.{IdentityNotFoundException, InvalidPasswordException}
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import parsers.{JsonBodyParser, JsonUtils}
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration, Logger}
import security._
import uk.ac.ncl.openlab.intake24.api.data._
import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SigninAttempt, SigninLogService, SurveyUserAlias}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SigninController @Inject()(silhouette: Environment[Intake24ApiEnv],
                                 emailProvider: EmailProvider,
                                 surveyAliasProvider: SurveyAliasProvider,
                                 urlTokenProvider: URLTokenProvider,
                                 signinLogService: SigninLogService,
                                 actorSystem: ActorSystem,
                                 rab: Intake24RestrictedActionBuilder,
                                 configuration: Configuration,
                                 jsonBodyParser: JsonBodyParser,
                                 val controllerComponents: ControllerComponents,
                                 implicit val executionContext: ExecutionContext) extends BaseController with JsonUtils with DatabaseErrorHandler {

  val accessTokenExpiryPeriod = configuration.get[Int]("intake24.security.accessTokenExpiryMinutes").minutes
  val refreshTokenExpiryPeriod = configuration.get[Int]("intake24.security.refreshTokenExpiryDays").days

  def logAttemptAsync(event: SigninAttempt) = {
    actorSystem.scheduler.scheduleOnce(0.seconds) {
      signinLogService.logSigninAttempt(event) match {
        case Right(()) => ()
        case Left(UnexpectedDatabaseError(e)) => Logger.error("Failed to log sign in attempt", e)
      }
    }
  }

  def getRemoteAddress(request: RequestHeader) = request.headers.get("X-Real-IP").getOrElse(request.remoteAddress)

  def getUserAgent(request: RequestHeader) = request.headers.get("User-Agent")

  def handleAuthResult(providerID: String, providerKey: String, authResult: Future[LoginInfo])(implicit request: RequestHeader): Future[Result] = {

    def logException(e: Throwable) = logAttemptAsync(SigninAttempt(None, getUserAgent(request), providerID, providerKey, false, None, Some(e.getClass.getSimpleName + ": " + e.getMessage)))

    authResult.flatMap {
      loginInfo =>
        silhouette.identityService.retrieve(loginInfo).flatMap {
          case Some(user) =>

            // GDPR: as long as we know some personal information (e-mail) might as well store the IP
            //       otherwise don't store the IP at all
            val remoteAddressOption = if (loginInfo.providerID == "email") Some(getRemoteAddress(request)) else None

            logAttemptAsync(SigninAttempt(remoteAddressOption, getUserAgent(request), providerID, providerKey, true, Some(user.userInfo.id), None))

            silhouette.authenticatorService.create(loginInfo).flatMap {
              t =>

                val customClaims = Json.obj("type" -> "refresh", "userId" -> user.userInfo.id)

                silhouette.authenticatorService.init(t.copy(customClaims = Some(customClaims), expirationDateTime = t.lastUsedDateTime.plusSeconds(refreshTokenExpiryPeriod.toSeconds.toInt))).map {
                  token =>
                    Ok(toJsonString(SigninResult(token))).as(ContentTypes.JSON)
                }
            }
          case None =>
            logAttemptAsync(SigninAttempt(None, getUserAgent(request), loginInfo.providerID, loginInfo.providerKey, false, None, Some("Authentication was successful, but identity service could not find user")))
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

  def signinWithAlias = Action.async(jsonBodyParser.parse[SurveyAliasCredentials]) {
    implicit request =>

      val credentials = request.body

      val providerKey = SurveyAliasUtils.toString(SurveyUserAlias(credentials.surveyId, credentials.userName))

      val authResult = surveyAliasProvider.authenticate(Credentials(providerKey, credentials.password))

      handleAuthResult(SurveyAliasProvider.ID, providerKey, authResult)
  }

  def signinWithEmail = Action.async(jsonBodyParser.parse[EmailCredentials]) {
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

  def refresh = rab.restrictRefresh {
    implicit request =>

      val jwt = request.subject.jwt

      silhouette.identityService.retrieve(jwt.loginInfo).flatMap {
        case Some(user) => silhouette.authenticatorService.create(jwt.loginInfo).flatMap {
          accessToken =>

            val customFields = user.userInfo.customFields.map { case (k, v) => Json.obj("name" -> k, "value" -> v) }

            val customClaims = user.userInfo.name match {
              case Some(name) => Json.obj("type" -> "access", "userId" -> user.userInfo.id, "roles" -> user.userInfo.roles.toList,
                "customFields" -> customFields, "name" -> name)
              case None => Json.obj("type" -> "access", "userId" -> user.userInfo.id, "roles" -> user.userInfo.roles.toList,
                "customFields" -> customFields)
            }

            /*
            // This code is for idle expiration of refresh tokens, disabled for simplicity

            val updatedRefreshToken = refreshEnv.authenticatorService.touch(refreshToken) match {
              case Left(x) => x
              case Right(x) => x
            }

            for (accessTokenValue <- accessEnv.authenticatorService.init(accessToken.copy(customClaims = Some(customClaims)))));
                 refreshTokenValue <- refreshEnv.authenticatorService.init(updatedRefreshToken))
             */

            silhouette.authenticatorService.init(accessToken.copy(customClaims = Some(customClaims), expirationDateTime = accessToken.lastUsedDateTime.plusSeconds(accessTokenExpiryPeriod.toSeconds.toInt))).map {
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
  }
}
