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

import akka.actor.ActorSystem
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import io.circe.syntax._
import models.{SaveUserSessionRequest, UserSessionResponse}
import org.slf4j.LoggerFactory
import parsers.{JsonBodyParser, JsonUtils}
import play.api.http.ContentTypes
import play.api.libs.ws.WSClient
import play.api.mvc._
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.data.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SurveyAdminService, UserAdminService}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{FoodPopularityService, SurveyService, UserSession, UserSessionDataService}
import uk.ac.ncl.openlab.intake24.surveydata.{SubmissionNotification, SurveySubmission}

import java.time.temporal.ChronoUnit
import java.time.{ZoneId, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class SubmissionResponseBody(followUpUrl: Option[String], redirectToFeedback: Boolean)

case class UserInfoResponseBody(id: Long, name: Option[String], recallNumber: Int, redirectToFeedback: Boolean,
                                maximumTotalSubmissionsReached: Boolean, maximumDailySubmissionsReached: Boolean)

class SurveyController @Inject()(service: SurveyService,
                                 ws: WSClient,
                                 userService: UserAdminService,
                                 surveyAdminService: SurveyAdminService,
                                 nutrientMappingService: NutrientMappingService,
                                 foodPopularityService: FoodPopularityService,
                                 actorSystem: ActorSystem,
                                 rab: Intake24RestrictedActionBuilder,
                                 playBodyParsers: PlayBodyParsers,
                                 jsonBodyParser: JsonBodyParser,
                                 userSessionDataService: UserSessionDataService,
                                 val controllerComponents: ControllerComponents,
                                 implicit val executionContext: ExecutionContext) extends BaseController
  with DatabaseErrorHandler with JsonUtils {

  private val logger = LoggerFactory.getLogger(classOf[SurveyController])

  def getPublicSurveyParameters(surveyId: String) = Action {
    translateDatabaseResult(service.getPublicSurveyParameters(surveyId))
  }

  def getSurveyFeedbackStyle(surveyId: String) = Action.async {
    _ =>
      Future {
        translateDatabaseResult(service.getSurveyFeedbackStyle(surveyId))
      }
  }

  def getSurveyParameters(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId), Roles.surveyRespondent(surveyId))(playBodyParsers.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.getSurveyParameters(surveyId))
      }
  }

  def getSurveyUserInfo(surveyId: String, clientTimeZone: Option[String]) = rab.restrictToRoles(Roles.surveyRespondent(surveyId))(playBodyParsers.empty) {
    request =>
      Future {
        val userId = request.subject.userId

        val zoneId = parseZoneId(clientTimeZone.getOrElse(null))

        val clientDayOfYear = ZonedDateTime.now(zoneId).getDayOfYear

        (for (surveyParameters <- service.getSurveyParameters(surveyId);
              user <- userService.getUserById(userId);
              currentSubmissionsCount <- service.getNumberOfSubmissionsForUser(surveyId, userId);
              numberOfSubmissionsToday <- service.getNumberOfSubmissionsOnDay(surveyId, userId, clientDayOfYear, zoneId.getId());
              followUp <- service.getSurveyFollowUp(surveyId))
          yield ((surveyParameters, user, currentSubmissionsCount, numberOfSubmissionsToday, followUp))) match {
          case Right((surveyParameters, user, currentSubmissionsCount, numberOfSubmissionsToday, followUp)) =>

            val redirectToFeedback = (currentSubmissionsCount >= surveyParameters.numberOfSurveysForFeedback) && followUp.showFeedback
            Ok(UserInfoResponseBody(user.id, user.name, currentSubmissionsCount + 1, redirectToFeedback,
              surveyParameters.maximumTotalSubmissions match {
                case Some(limit) => currentSubmissionsCount >= limit
                case None => false
              },
              numberOfSubmissionsToday >= surveyParameters.maximumDailySubmissions).asJson.noSpaces).as(ContentTypes.JSON)

          case Left(error) => translateDatabaseError(error)
        }
      }
  }

  def getSurveyFollowUp(surveyId: String) = rab.restrictToRoles(Roles.surveyRespondent(surveyId))(playBodyParsers.empty) {
    request =>
      Future {
        val userId = request.subject.userId

        val result = for (
          userNameOpt <- userService.getSurveyUserAliases(Seq(userId), surveyId).right.map(_.get(userId).map(_.userName)).right;
          followUp <- service.getSurveyFollowUp(surveyId).right
        ) yield {

          if (userNameOpt.isEmpty)
            logger.warn(s"Survey user has no survey alias (for external follow up URL): $userId")

          val followUpUrlWithUserName = for (userName <- userNameOpt;
                                             followUpUrl <- followUp.followUpUrl)
            yield followUpUrl.replace("[intake24_username_value]", userName)

          followUp.copy(followUpUrl = followUpUrlWithUserName)
        }

        translateDatabaseResult(result)
      }
  }

  def saveSession(surveyId: String) = rab.restrictToRoles(Roles.surveyRespondent(surveyId))(jsonBodyParser.parse[SaveUserSessionRequest]) {
    request =>
      Future {
        val userId = request.subject.userId
        translateDatabaseResult(userSessionDataService.save(UserSession(userId, surveyId, request.body.data)))
      }
  }

  def getSession(surveyId: String) = rab.restrictToRoles(Roles.surveyRespondent(surveyId))(playBodyParsers.empty) {
    request =>
      Future {
        val userId = request.subject.userId
        val r = userSessionDataService.get(surveyId, userId)
          .map(s => UserSessionResponse(Some(s)))
          .getOrElse(UserSessionResponse(None))
        translateDatabaseResult(Right(r))
      }
  }

  def cleanSession(surveyId: String) = rab.restrictToRoles(Roles.surveyRespondent(surveyId))(playBodyParsers.empty) {
    request =>
      Future {
        val userId = request.subject.userId
        translateDatabaseResult(userSessionDataService.clean(surveyId, userId))
      }
  }

  def isTooEarly(lastSubmissionTime: Option[ZonedDateTime], minimumInterval: Int): Boolean = lastSubmissionTime match {
    case Some(time) =>
      val interval = ChronoUnit.SECONDS.between(time, ZonedDateTime.now())
      interval < minimumInterval
    case None => false
  }

  def parseZoneId(zone: String): ZoneId = try {
    ZoneId.of(zone)
  } catch {
    case _: RuntimeException =>
      ZoneId.systemDefault()
  }

  def submitSurvey(surveyId: String) = rab.restrictToRoles(Roles.surveyRespondent(surveyId))(jsonBodyParser.parse[SurveySubmission]) {
    request =>
      Future {
        val userId = request.subject.userId

        val zoneId = parseZoneId(request.body.timeZone)

        val clientDayOfYear = ZonedDateTime.now(zoneId).getDayOfYear

        val result = for (surveyParameters <- service.getSurveyParameters(surveyId);
                          currentSubmissionsCount <- service.getNumberOfSubmissionsForUser(surveyId, userId);
                          numberOfSubmissionsToday <- service.getNumberOfSubmissionsOnDay(surveyId, userId, clientDayOfYear, zoneId.getId());
                          lastSubmissionTime <- service.getLastSubmissionTime(surveyId, userId);
                          userNameOpt <- userService.getSurveyUserAliases(Seq(userId), surveyId).map(_.get(userId).map(_.userName));
                          followUp <- service.getSurveyFollowUp(surveyId)) yield {
          if (surveyParameters.state != "running")
            Forbidden(toJsonString(ErrorDescription("SurveyNotRunning", "Survey not accepting submissions at this time")))
          else if (numberOfSubmissionsToday >= surveyParameters.maximumDailySubmissions)
            TooManyRequests(toJsonString(ErrorDescription("MaximumDailySubmissions", "Maximum daily submissions reached, try again tomorrow")))
          else if (surveyParameters.maximumTotalSubmissions match {
            case Some(limit) => currentSubmissionsCount >= limit
            case None => false
          })
            Forbidden(toJsonString(ErrorDescription("MaximumSubmissions", "Maximum number of submissions reached")))
          else if (isTooEarly(lastSubmissionTime, surveyParameters.minimumSubmissionInterval))
            TooManyRequests(toJsonString(ErrorDescription("MinimumSubmissionInterval", "Minimum submission interval not met, try again later")))
          else {
            val userId = request.subject.userId

            // No reason to keep the user waiting for the database result because reporting nutrient mapping or
            // database errors to the user is not helpful at this point.
            // Schedule submission asynchronously to release the request immediately and log errors server-side instead.
            actorSystem.scheduler.scheduleOnce(0.seconds) {

              val foodCodes = request.body.meals.foldLeft(List[String]()) {
                (acc, meal) =>
                  meal.foods.foldLeft(acc) {
                    (acc, food) => food.code :: acc
                  }
              }

              val result = for (nutrientMappedSubmission <- nutrientMappingService.mapSurveySubmission(request.body, surveyParameters.localeId).right;
                                submissionId <- service.createSubmission(userId, surveyId, nutrientMappedSubmission).right;
                                _ <- foodPopularityService.incrementPopularityCount(foodCodes).right) yield ((nutrientMappedSubmission, submissionId))
              result match {
                case Right((nutrientMappedSubmission, submissionId)) =>
                  surveyAdminService.getSurveyParameters(surveyId) match {
                    case Right(surveyParameters) =>
                      surveyParameters.submissionNotificationUrl.foreach {
                        notificationUrl =>

                          (for (userParams <- userService.getUserById(userId);
                                userAlias <- userService.getSurveyUserAliases(Seq(userId), surveyId))
                            yield ((userParams, userAlias))) match {
                            case Right((userParams, userAlias)) =>
                              logger.debug("Sending survey submission notification")

                              val payload = SubmissionNotification(userParams.id, surveyId, userAlias.get(userId).map(_.userName).getOrElse(null),
                                userParams.customFields, nutrientMappedSubmission.startTime, nutrientMappedSubmission.endTime,
                                nutrientMappedSubmission.uxSessionId, submissionId, nutrientMappedSubmission.customData, nutrientMappedSubmission.meals).asJson.noSpaces

                              ws.url(notificationUrl)
                                .withHttpHeaders("Content-Type" -> "application/json; charset=utf-8")
                                .withBody(payload)
                                .execute("POST")
                                .onComplete {
                                  case Success(response) =>
                                    if (response.status == 200)
                                      logger.debug(s"Survey submission notification sent to $notificationUrl")
                                    else
                                      logger.error(s"Survey submission notification sent, but request failed with code ${response.status}")
                                  case Failure(e) =>
                                    logger.error("Failed to send survey submission notification", e)
                                }

                            case Left(error) => logger.error(s"Could not get user data for user $userId", error.exception)
                          }


                      }
                    case Left(error) => logger.error(s"Could not get survey parameters for survey $surveyId", error.exception)
                  }

                case Left(error) => logger.error(s"Could not save survey submission!", error.exception)

              }
            }

            if (userNameOpt.isEmpty)
              logger.warn(s"Survey user has no survey alias (for external follow up URL): $userId")

            val submissionThresholdReached = (currentSubmissionsCount + 1) >= surveyParameters.numberOfSurveysForFeedback

            val followUpUrlWithUserName =
              if (submissionThresholdReached) {
                for (userName <- userNameOpt;
                     followUpUrl <- followUp.followUpUrl)
                  yield followUpUrl.replace("[intake24_username_value]", userName)
              }
              else
                None

            val redirectToFeedback = submissionThresholdReached && followUp.showFeedback

            Ok(SubmissionResponseBody(followUpUrlWithUserName, redirectToFeedback).asJson.noSpaces).as(ContentTypes.JSON)
          }
        }

        translateDatabaseHttpResult(result)
      }
  }
}
