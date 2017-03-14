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
import models.RefreshSubject
import parsers.UpickleUtil
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, BodyParsers, Controller}
import security._
import uk.ac.ncl.openlab.intake24.api.shared.{ErrorDescription, RefreshResult, SigninResult, Credentials => Intake24Credentials}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SurveyAdminService, UserAdminService}
import upickle.default._

import scala.concurrent.Future

class SigninController @Inject()(@Named("refresh") refreshEnv: Environment[Intake24ApiEnv], @Named("access") accessEnv: Environment[Intake24ApiEnv],
                                 credentialsProvider: CredentialsProvider, userAdminService: UserAdminService, surveyAdminService: SurveyAdminService,
                                 deadbolt: DeadboltActionsAdapter)
  extends Controller with UpickleUtil {

  def signin = Action.async(upickleBodyParser[Intake24Credentials]) {
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

  // TODO: captcha to prevent new user spam
  def genUser(surveyId: String) = Action.async(BodyParsers.parse.empty) {
    request =>
      Future {


        /*
       if (survey_name.isEmpty())
      throw new RuntimeException("This feature is not applicable to system-wide users");

    try {
      SurveyParameters parameters = dataStore.getSurveyParameters(survey_name);

      if (!parameters.allowGenUsers)
        throw new RuntimeException("Automatically generated user records are not allowed for this survey");

      final String counterName = survey_name + "_gen_user_counter";

      int counter = Integer.parseInt(dataStore.getGlobalValue(counterName).getOrElse("0"));

      StringBuilder psb = new StringBuilder();

      ByteSource bytes = rng.nextBytes(passwordLength);

      for (int i = 0; i < passwordLength; i++) {
        int index = ((int) (bytes.getBytes()[i]) + 128) % passwordChars.length();
        psb.append(passwordChars.charAt(index));
      }

      ByteSource salt = rng.nextBytes();

      String password = psb.toString();

      String passwordHashBase64 = new Sha256Hash(password, salt, 1024).toBase64();
      String passwordSaltBase64 = salt.toBase64();

      Set<String> roles = new HashSet<String>();
      roles.add("respondent");

      Set<String> permissions = new HashSet<String>();
      permissions.add("processSurvey:" + survey_name);

      int retries = 20;
      boolean addUserOk = false;
      String username = "";

      while (retries > 0) {
        counter++;
        username = survey_name + counter;

        try {
          dataStore.addUser(survey_name, new SecureUserRecord(username, passwordHashBase64, passwordSaltBase64, Option.<String>none(),
              Option.<String>none(), Option.<String>none(), roles, permissions, new HashMap<String, String>()));
          addUserOk = true;
          break;
        } catch (DataStoreException | DuplicateKeyException e) {
          continue;
        }
      }

      if (!addUserOk)
        throw new RuntimeException("Could not find a unique user name in 20 attempts");

      dataStore.setGlobalValue(counterName, Integer.toString(counter));

      return new UserRecord(username, password, Option.<String>none(), Option.<String>none(), Option.<String>none(), new HashMap<String, String>());
       */
        Ok
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
                  serialisedAccessToken => (Ok(write(RefreshResult(serialisedAccessToken))).as(ContentTypes.JSON))
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

        case _ => Future.successful(Unauthorized.withHeaders(("WWW-Authenticate", "X-Auth-Token")))
      }
  }
}