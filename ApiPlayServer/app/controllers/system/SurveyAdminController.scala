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

package controllers.system

import javax.inject.Inject

import parsers.UpickleUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller}
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.api.shared.CreateSurveyRequest
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SurveyAdminService
import upickle.default._

import scala.concurrent.Future


class SurveyAdminController @Inject()(service: SurveyAdminService, deadbolt: DeadboltActionsAdapter) extends Controller
  with SystemDatabaseErrorHandler with UpickleUtil {

  def createSurvey() = deadbolt.restrictAccess(Roles.superuser)(upickleBodyParser[CreateSurveyRequest]) {
    request =>
      Future {
        val body = request.body
        translateDatabaseResult(service.createSurvey(body.surveyId, body.schemeId, body.localeId, body.allowGeneratedUsers, body.externalFollowUpUrl, body.supportEmail))
      }
  }

  def deleteSurvey(surveyId: String) = deadbolt.restrictAccess(Roles.superuser)(BodyParsers.parse.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.deleteSurvey(surveyId))
      }
  }
}
