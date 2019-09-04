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
import parsers.{HtmlSanitisePolicy, JsonBodyParser, JsonUtils}
import play.api.mvc.{BaseController, BodyParsers, ControllerComponents, PlayBodyParsers}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{StaffSurveyUpdate, SurveyAdminService, SurveyParametersIn}

import scala.concurrent.{ExecutionContext, Future}


class SurveyAdminController @Inject()(service: SurveyAdminService,
                                      rab: Intake24RestrictedActionBuilder,
                                      playBodyParsers: PlayBodyParsers,
                                      jsonBodyParser: JsonBodyParser,
                                      val controllerComponents: ControllerComponents,
                                      implicit val executionContext: ExecutionContext) extends BaseController
  with DatabaseErrorHandler with JsonUtils with SurveyAuthChecks {

  private def sanitiseSurvey(surveyParametersIn: SurveyParametersIn, strict: Boolean = true): SurveyParametersIn = {
    val description =
      if (strict) surveyParametersIn.description.map(d => HtmlSanitisePolicy.sanitise(d))
      else surveyParametersIn.description.map(d => HtmlSanitisePolicy.easedSanitise(d))

    val finalPageHtml =
      if (strict) surveyParametersIn.finalPageHtml.map(html => HtmlSanitisePolicy.sanitise(html))
      else surveyParametersIn.finalPageHtml.map(html => HtmlSanitisePolicy.easedSanitise(html))

    SurveyParametersIn(surveyParametersIn.id, surveyParametersIn.schemeId, surveyParametersIn.localeId,
      surveyParametersIn.state, surveyParametersIn.startDate, surveyParametersIn.endDate,
      surveyParametersIn.allowGeneratedUsers, surveyParametersIn.externalFollowUpURL,
      surveyParametersIn.supportEmail, description, finalPageHtml, surveyParametersIn.submissionNotificationUrl,
      surveyParametersIn.feedbackEnabled, surveyParametersIn.numberOfSubmissionsForFeedback,
      surveyParametersIn.storeUserSessionOnServer)
  }

  def createSurvey() = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin)(jsonBodyParser.parse[SurveyParametersIn]) {
    request =>
      Future {
        translateDatabaseResult(service.createSurvey(sanitiseSurvey(request.body, false)))
      }
  }

  def updateSurvey(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(jsonBodyParser.parse[SurveyParametersIn]) {
    request =>
      Future {

        val subject = request.subject

        val isStaff = !subject.roles.contains(Roles.superuser) && !subject.roles.contains(Roles.surveyAdmin)

        val params = sanitiseSurvey(request.body, isStaff)

        if (isStaff)
        // Survey staff is not allowed to change survey ID, scheme, locale and generated user settings
        // FIXME: better split into different endpoints for cleaner authorization
          translateDatabaseResult(service.staffUpdateSurvey(surveyId,
            StaffSurveyUpdate(params.startDate, params.endDate, params.state,
              params.externalFollowUpURL, params.supportEmail, params.description, params.finalPageHtml)))
        else
          translateDatabaseResult(service.updateSurvey(surveyId, params))
      }
  }

  def validateSurveyId(id: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin)(playBodyParsers.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.validateSurveyId(id))
      }
  }

  def list() = rab.restrictAccess(canListSurveys) {
    request =>
      Future {
        val filteredResult = service.listSurveys().right.map {
          _.filter {
            survey =>
              request.subject.roles.exists(r => r == Roles.surveyStaff(survey.id) || r == Roles.superuser || r == Roles.surveyAdmin)
          }
        }

        translateDatabaseResult(filteredResult)
      }
  }

  def getSurvey(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(playBodyParsers.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.getSurvey(surveyId))
      }
  }

  def deleteSurvey(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin)(playBodyParsers.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.deleteSurvey(surveyId))
      }
  }
}
