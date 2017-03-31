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

import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import parsers.JsonUtils
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, BodyParsers, Controller}
import security.{DeadboltActionsAdapter, Intake24UserKey, Roles}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService
import uk.ac.ncl.openlab.intake24.services.systemdb.user.SurveyService
import uk.ac.ncl.openlab.intake24.surveydata.SurveySubmission

import scala.concurrent.Future

class SurveyController @Inject()(service: SurveyService,
                                 nutrientMappingService: NutrientMappingService,
                                 deadbolt: DeadboltActionsAdapter) extends Controller
  with DatabaseErrorHandler with JsonUtils {

  def getPublicSurveyParameters(surveyId: String) = Action {
    translateDatabaseResult(service.getPublicSurveyParameters(surveyId))
  }

  def getSurveyParameters(surveyId: String) = deadbolt.restrictToRoles(Roles.surveyRespondent(surveyId))(BodyParsers.parse.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.getSurveyParameters(surveyId))
      }
  }

  def submitSurvey(surveyId: String) = deadbolt.restrictToRoles(Roles.surveyRespondent(surveyId))(jsonBodyParser[SurveySubmission]) {
    request =>
      Future {
        service.getSurveyParameters(surveyId) match {
          case Right(params) =>
            if (params.state != "running")
              Forbidden(toJsonString(ErrorDescription("SurveyNotRunning", "Survey not accepting submissions at this time")))
            else {
              val userName = Intake24UserKey.fromString(request.subject.get.identifier).userName

              val result = for (nutrientMappedSubmission <- nutrientMappingService.mapSurveySubmission(request.body, params.localeId).right;
                                _ <- service.createSubmission(surveyId, userName, nutrientMappedSubmission).right)
                yield ()
              translateDatabaseResult(result)
            }
          case Left(error) => translateDatabaseError(error)
        }
      }
  }
}