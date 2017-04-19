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

import akka.actor.ActorSystem
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import models.Intake24Subject
import parsers.JsonUtils
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, BodyParsers, Controller}
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.user.SurveyService
import uk.ac.ncl.openlab.intake24.surveydata.SurveySubmission

import scala.concurrent.Future
import scala.concurrent.duration._

case class SurveyFollowUp(url: Option[String])

class SurveyController @Inject()(service: SurveyService,
                                 userService: UserAdminService,
                                 nutrientMappingService: NutrientMappingService,
                                 actorSystem: ActorSystem,
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

  def getSurveyFollowUp(surveyId: String) = deadbolt.restrictToRoles(Roles.surveyRespondent(surveyId))(BodyParsers.parse.empty) {
    request =>
      Future {
        val userId = request.subject.get.asInstanceOf[Intake24Subject].userId

        val result = for (
          userNameOpt <- userService.getSurveyUserAliases(Seq(userId), surveyId).right.map(_.get(userId)).right;
          followUpUrlOpt <- service.getSurveyFollowUpURL(surveyId).right
        ) yield {

          if (userNameOpt.isEmpty)
            Logger.warn(s"Survey user has no survey alias (for external follow up URL): $userId")

          for (userName <- userNameOpt;
               followUpUrl <- followUpUrlOpt)
            yield followUpUrl.replace("[intake24_username_value]", userName)
        }

        translateDatabaseResult(result.right.map(SurveyFollowUp(_)))
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
              val userId = request.subject.get.asInstanceOf[Intake24Subject].userId

              // No reason to keep the user waiting for the database result because reporting nutrient mapping or
              // database errors to the user is not helpful at this point.
              // Schedule submission asynchronously to release the request immediately and log errors server-side instead.
              actorSystem.scheduler.scheduleOnce(0 seconds) {
                val result = for (nutrientMappedSubmission <- nutrientMappingService.mapSurveySubmission(request.body, params.localeId).right;
                                  _ <- service.createSubmission(userId, surveyId, nutrientMappedSubmission).right) yield ()

                result match {
                  case Right(()) => ()
                  case Left(e) => Logger.error("Failed to process survey submission", e.exception)
                }
              }

              Ok
            }
          case Left(error) => translateDatabaseError(error)
        }
      }
  }
}
