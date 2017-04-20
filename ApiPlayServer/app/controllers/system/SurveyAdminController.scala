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

import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import models.Intake24Subject
import parsers.JsonUtils
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller}
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24.api.shared.CreateSurveyRequest
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SurveyAdminService, SurveyParametersIn}

import scala.concurrent.Future


class SurveyAdminController @Inject()(service: SurveyAdminService, deadbolt: DeadboltActionsAdapter) extends Controller
  with DatabaseErrorHandler with JsonUtils {

  def createSurvey() = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[CreateSurveyRequest]) {
    request =>
      Future {
        val body = request.body
        translateDatabaseResult(service.createSurvey(SurveyParametersIn(body.id, body.startDate, body.endDate,
          body.schemeId, body.localeId, body.allowGeneratedUsers, body.externalFollowUpURL, body.supportEmail)))
      }
  }

  def updateSurvey(surveyId: String) = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[CreateSurveyRequest]) {
    request =>
      Future {
        val body = request.body
        translateDatabaseResult(service.updateSurvey(surveyId, SurveyParametersIn(body.id, body.startDate, body.endDate,
          body.schemeId, body.localeId, body.allowGeneratedUsers, body.externalFollowUpURL, body.supportEmail)))
      }
  }

  def validateSurveyId(id: String) = deadbolt.restrictToRoles(Roles.superuser)(BodyParsers.parse.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.validateSurveyId(id))
      }
  }

  // FIXME: Restrict to staff & admins only after Deadbolt replacement

  def list() = deadbolt.restrictToAuthenticated {
    request =>
      Future {

        val user = request.subject.get

        val filteredResult = service.listSurveys().right.map {
          _.filter {
            survey =>
              user.roles.exists( r=> r.name == Roles.surveyStaff(survey.id) || r.name == Roles.superuser || r.name == Roles.surveyAdmin)
          }
        }

        translateDatabaseResult(filteredResult)
      }
  }

  def getSurvey(surveyId: String) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(BodyParsers.parse.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.getSurvey(surveyId))
      }
  }

  def deleteSurvey(surveyId: String) = deadbolt.restrictToRoles(Roles.superuser)(BodyParsers.parse.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.deleteSurvey(surveyId))
      }
  }
}
