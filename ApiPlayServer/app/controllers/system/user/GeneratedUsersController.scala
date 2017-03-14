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

import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import controllers.DatabaseErrorHandler
import parsers.UpickleUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import security.Roles
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SecureUserRecord, SurveyAdminService, UserAdminService}
import upickle.default._

import scala.concurrent.Future
import scala.util.Random

case class GeneratedCredentials(userName: String, password: String)

class GeneratedUsersController @Inject()(userAdminService: UserAdminService,
                                         surveyAdminService: SurveyAdminService,
                                         passwordHasherRegistry: PasswordHasherRegistry) extends Controller
  with DatabaseErrorHandler with UpickleUtil {

  private def generateCredentials(surveyId: String, counter: Int): GeneratedCredentials = {
    val userName = surveyId + Integer.toString(counter)
    val password = Random.alphanumeric.take(5).mkString.toLowerCase

    GeneratedCredentials(userName, password)
  }

  private def mkSecureUserRecord(surveyId: String, credentials: GeneratedCredentials): SecureUserRecord = {
    val pwInfo = passwordHasherRegistry.current.hash(credentials.password)

    SecureUserRecord(credentials.userName, pwInfo.password, pwInfo.salt.get, pwInfo.hasher, None, None, None, Set(Roles.surveyRespondent(surveyId)), Set(), Map())
  }

  // TODO: captcha to prevent new user spam
  def generateUser(surveyId: String) = Action.async(BodyParsers.parse.empty) {
    request =>
      Future {
        surveyAdminService.getSurveyParameters(surveyId) match {
          case Right(params) =>
            if (!params.allowGeneratedUsers)
              Forbidden(write(ErrorDescription("GenUsersNotAllowed", "Generated users are not allowed for this survey")))
            else {
              val result = userAdminService.nextGeneratedUserId(surveyId).right.flatMap {
                counter =>
                  val credentials = generateCredentials(surveyId, counter)
                  val secureUserRecord = mkSecureUserRecord(surveyId, credentials)

                  userAdminService.createUser(Some(surveyId), secureUserRecord).right.map(_ => credentials)
              }

              translateDatabaseResult(result)
            }
          case Left(error) =>
            translateDatabaseError(error)
        }
      }
  }
}
