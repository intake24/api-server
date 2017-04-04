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
import io.circe.generic.auto._
import parsers.JsonUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import security.Roles
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._

import scala.concurrent.Future
import scala.util.Random

case class GeneratedCredentials(userName: String, password: String)

class GeneratedUsersController @Inject()(userAdminService: UserAdminService,
                                         surveyAdminService: SurveyAdminService,
                                         passwordHasherRegistry: PasswordHasherRegistry) extends Controller
  with DatabaseErrorHandler with JsonUtils {

  private def generateCredentials(surveyId: String, counter: Int): GeneratedCredentials = {
    val userName = surveyId + Integer.toString(counter)
    val password = Random.alphanumeric.take(5).mkString.toLowerCase

    GeneratedCredentials(userName, password)
  }

  private def mkNewUserRecord(surveyId: String, credentials: GeneratedCredentials): NewUserWithAlias = {
    val pwInfo = passwordHasherRegistry.current.hash(credentials.password)

    NewUserWithAlias(
      SurveyUserAlias(surveyId, credentials.userName),
      UserInfo(None, None, None, Set(Roles.surveyRespondent(surveyId)), Map()),
      SecurePassword(pwInfo.password, pwInfo.salt.get, pwInfo.hasher)
    )
  }

  // TODO: captcha to prevent new user spam
  def generateUser(surveyId: String) = Action.async(BodyParsers.parse.empty) {
    request =>
      Future {
        surveyAdminService.getSurveyParameters(surveyId) match {
          case Right(params) =>
            if (!params.allowGeneratedUsers)
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
}
