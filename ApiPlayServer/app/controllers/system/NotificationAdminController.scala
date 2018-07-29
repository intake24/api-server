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

import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import javax.inject.Inject
import parsers.{JsonUtils, NotificationScheduleCSVParser}
import play.api.Configuration
import play.api.http.ContentTypes
import play.api.libs.Files
import play.api.mvc._
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.data._
import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.services.systemdb.notifications.{NewNotification, NotificationScheduleDataService, RecallNotificationRequest}

import scala.concurrent.{ExecutionContext, Future}

class NotificationAdminController @Inject()(service: NotificationScheduleDataService,
                                            userService: UserAdminService,
                                            configuration: Configuration,
                                            rab: Intake24RestrictedActionBuilder,
                                            playBodyParsers: PlayBodyParsers,
                                            val controllerComponents: ControllerComponents,
                                            implicit val executionContext: ExecutionContext) extends BaseController
  with DatabaseErrorHandler with JsonUtils {


  private def doCreateOrUpdate(surveyId: String, notifications: Seq[RecallNotificationRequest]): Result = {
    val ns = notifications.map { n =>
      userService.getUserByEmail(n.userEmail) match {
        case Right(u) => Right(NewNotification(u.id, Some(surveyId), n.dateTime, n.notificationType))
        case Left(e) => Left(n.userEmail)
      }
    }

    if (ns.exists(_.isLeft)) {
      translateDatabaseError(UnexpectedDatabaseError(new Exception(s"Users with the following emails were not found: ${ns.filter(_.isLeft).map(_.left.get) mkString ", "}")))
    } else {
      translateDatabaseResult(service.batchCreate(ns.map(_.right.get)))
    }
  }

  private def uploadCSV(formData: MultipartFormData[Files.TemporaryFile], surveyId: String): Result = {
    if (formData.files.length != 1)
      BadRequest(toJsonString(ErrorDescription("BadRequest", s"Expected exactly one file attachment, got ${formData.files.length}"))).as(ContentTypes.JSON)
    else {
      NotificationScheduleCSVParser.parseFile(formData.files(0).ref.path.toFile) match {
        case Right(csvRecords) =>
          doCreateOrUpdate(surveyId, csvRecords)
        case Left(error) =>
          BadRequest(toJsonString(ErrorDescription("InvalidCSV", error)))
      }
    }
  }

  def uploadNotificationsCSV(surveyId: String) = rab.restrictToRoles(Roles.superuser, Roles.surveyAdmin, Roles.surveyStaff(surveyId))(playBodyParsers.multipartFormData) {
    request =>
      Future {
        uploadCSV(request.body, surveyId)
      }
  }

}
