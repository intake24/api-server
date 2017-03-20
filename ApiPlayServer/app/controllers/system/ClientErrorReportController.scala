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

import java.time.Instant
import javax.inject.Inject

import controllers.DatabaseErrorHandler
import parsers.JsonUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{ClientErrorService, NewClientErrorReport}
import io.circe.generic.auto._

import scala.concurrent.Future

class ClientErrorReportController @Inject()(service: ClientErrorService) extends Controller with DatabaseErrorHandler with JsonUtils {

  case class ForwardErrorReport(userId: Option[String], surveyId: Option[String], stackTrace: Seq[String], surveyStateJSON: String)

  // TODO: only allow requests from the frontend servers
  def reportError() = Action.async(jsonBodyParser[ForwardErrorReport]) {
    request =>
      Future {
        translateDatabaseResult(service.submitErrorReport(NewClientErrorReport(request.body.userId, request.body.surveyId, Instant.now(), request.body.stackTrace, request.body.surveyStateJSON)))
      }
  }
}
