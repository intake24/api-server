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

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import javax.inject.Inject

import controllers.DatabaseErrorHandler
import parsers.{SurveyCSVExporter, UpickleUtil}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller}
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, SurveyAdminService}
import upickle.default._

import scala.concurrent.Future


class DataExportController @Inject()(service: DataExportService, surveyAdminService: SurveyAdminService, foodGroupsAdminService: FoodGroupsAdminService, deadbolt: DeadboltActionsAdapter) extends Controller
  with DatabaseErrorHandler with UpickleUtil {


  def getSurveySubmissions(surveyId: String, dateFrom: String, dateTo: String, offset: Int, limit: Int) = deadbolt.restrictAccess(Roles.superuser, Roles.surveyStaff(surveyId))(BodyParsers.parse.empty) {
    _ =>
      Future {

        try {
          val parsedFrom = Instant.parse(dateFrom)
          val parsedTo = Instant.parse(dateTo)

          translateDatabaseResult(service.getSurveySubmissions(surveyId, parsedFrom, parsedTo, offset, limit))
        } catch {
          case e: DateTimeParseException => BadRequest(write(ErrorDescription("DateFormat", "Failed to parse date parameter. Expected a UTC date in ISO 8601 format, e.g. '2017-02-15T16:40:30Z'.")))
        }
      }
  }

  def getSurveySubmissionsAsCSV(surveyId: String, dateFrom: String, dateTo: String, offset: Int, limit: Int) = deadbolt.restrictAccess(Roles.superuser, Roles.surveyStaff(surveyId))(BodyParsers.parse.empty) {
    _ =>
      Future {

        try {
          val parsedFrom = Instant.parse(dateFrom)
          val parsedTo = Instant.parse(dateTo)

          val data = for (params <- surveyAdminService.getSurveyParameters(surveyId).right;
                          localNutrients <- surveyAdminService.getLocalNutrientTypes(params.localeId).right;
                          dataScheme <- surveyAdminService.getCustomDataScheme(params.schemeId).right;
                          foodGroups <- foodGroupsAdminService.listFoodGroups(params.localeId).right;
                          submissions <- service.getSurveySubmissions(surveyId, parsedFrom, parsedTo, offset, limit).right) yield ((localNutrients, dataScheme, foodGroups, submissions))

          data match {
            case Right((localNutrients, dataScheme, foodGroups, submissions)) =>
              SurveyCSVExporter.exportSurveySubmissions(dataScheme, foodGroups, localNutrients, submissions) match {
                case Right(csvFile) =>
                  val dateStamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.ofInstant(Clock.systemUTC().instant(), ZoneId.systemDefault).withNano(0)).replace(":", "-").replace("T", "-")
                  Ok.sendFile(csvFile, fileName = _ => s"intake24-$surveyId-data-$dateStamp.csv", onClose = () => csvFile.delete())
                case Left(exportError) => InternalServerError(write(ErrorDescription("ExportError", exportError)))
              }
            case Left(databaseError) => translateDatabaseError(databaseError)
          }


        } catch {
          case e: DateTimeParseException => BadRequest(write(ErrorDescription("DateFormat", "Failed to parse date parameter. Expected a UTC date in ISO 8601 format, e.g. '2017-02-15T16:40:30Z'.")))
        }
      }

  }
}
