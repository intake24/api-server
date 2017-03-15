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

import java.time.Instant
import javax.inject.Inject

import controllers.DatabaseErrorHandler
import parsers.UpickleUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import security.{DeadboltActionsAdapter, Intake24UserKey}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{NewGWTClientErrorReport, GWTClientErrorService}

import scala.concurrent.Future

case class GWTClientErrorReportRequest(gwtPermutationStrongName: String, exceptionChainJSON: String, surveyStateJSON: String)

class GWTClientErrorReportController @Inject()(service: GWTClientErrorService,
                                               deadbolt: DeadboltActionsAdapter) extends Controller
  with DatabaseErrorHandler with UpickleUtil {


  def reportError() = deadbolt.restrictToAuthenticated(upickleBodyParser[GWTClientErrorReportRequest]) {
    request =>
      Future {
        val user = Intake24UserKey.fromString(request.subject.get.identifier)
        translateDatabaseResult(service.submitErrorReport(NewGWTClientErrorReport(Some(user.userName), user.surveyId, Instant.now(), request.body.gwtPermutationStrongName,
          request.body.exceptionChainJSON, request.body.surveyStateJSON)))
      }
  }

}
