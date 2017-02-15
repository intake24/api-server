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

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import javax.inject.Inject

import parsers.UpickleUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller}
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.DataExportService
import upickle.default._

import scala.concurrent.Future


class DataExportController @Inject()(service: DataExportService, deadbolt: DeadboltActionsAdapter) extends Controller
  with SystemDatabaseErrorHandler with UpickleUtil {


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
}
