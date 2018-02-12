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

package controllers.temp

import javax.inject.Inject

import controllers.DatabaseErrorHandler
import parsers.JsonBodyParser
import play.api.Logger
import play.api.mvc.{BaseController, BodyParsers, ControllerComponents, PlayBodyParsers}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.surveydata.SubmissionNotification
import io.circe.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

class SubmissionNotificationListener @Inject()(rab: Intake24RestrictedActionBuilder,
                                               val controllerComponents: ControllerComponents,
                                               implicit val executionContext: ExecutionContext,
                                               playBodyParsers: PlayBodyParsers,
                                               jsonBodyParser: JsonBodyParser) extends BaseController
  with DatabaseErrorHandler {

  def notifySubmission() = Action.async(playBodyParsers.tolerantText) {
    request =>
      Future {

        Logger.debug("Received survey submission notification!")
        Logger.debug(request.body.toString)

        Ok
      }
  }

}
