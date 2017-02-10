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

import java.io.FileInputStream
import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import net.scran24.datastore.UserRecordCSV
import parsers.UpickleUtil
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller, Result}
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SecureUserRecord, UserAdminService}
import upickle.default._

import scala.collection.JavaConverters._
import scala.concurrent.Future

case class UserRecordWithPermissions(userName: String, password: String, name: Option[String], email: Option[String], phone: Option[String], customFields: Map[String, String], roles: Set[String], permissions: Set[String])

case class CreateOrUpdateGlobalUsersRequest(userRecords: Seq[UserRecordWithPermissions])

case class CreateOrUpdateUsersRequest(userRecords: Seq[UserRecord])

case class UserRecord(userName: String, password: String, name: Option[String], email: Option[String], phone: Option[String], customFields: Map[String, String])

class UserAdminController @Inject()(service: UserAdminService, passwordHasherRegistry: PasswordHasherRegistry, deadbolt: DeadboltActionsAdapter) extends Controller
  with SystemDatabaseErrorHandler with UpickleUtil {

  private def doCreateOrUpdate(surveyId: Option[String], userRecords: Seq[UserRecordWithPermissions]): Result = {
    val hasher = passwordHasherRegistry.current

    val secureUserRecords = userRecords.map {
      record =>
        val passwordInfo = hasher.hash(record.password)
        SecureUserRecord(record.userName, passwordInfo.password, passwordInfo.salt.get, passwordInfo.hasher, record.name, record.email, record.phone, record.roles, record.permissions, record.customFields)
    }

    translateDatabaseResult(service.createOrUpdateUsers(None, secureUserRecords))

  }

  def createOrUpdateGlobalUsers() = deadbolt.restrictAccess(Roles.superuser)(upickleBodyParser[CreateOrUpdateGlobalUsersRequest]) {
    request =>
      Future {
        doCreateOrUpdate(None, request.body.userRecords)
      }
  }

  def uploadSurveyStaffCSV(surveyId: String) = deadbolt.restrictAccess(Roles.superuser, Roles.surveyStaff(surveyId))(BodyParsers.parse.multipartFormData) {
    request =>
      Future {
        val formData = request.body

        if (formData.files.length != 1)
          BadRequest(write(ErrorDescription("BadRequest", s"Expected exactly one file attachment, got ${formData.files.length}"))).as(ContentTypes.JSON)
        else {
          UserRecordsCSVParser.parseFile(formData.files(0).ref.file) match {
            case Right(records) =>
              val recordsWithPermissions = records.map {
                record =>
                  UserRecordWithPermissions(record.userName, record.password, record.name, record.email, record.phone, record.customFields, Set(Roles.surveyStaff(surveyId)), Set())
              }
              doCreateOrUpdate(Some(surveyId), recordsWithPermissions)
            case Left(error) =>
              BadRequest(write(ErrorDescription("InvalidCSV", error)))
          }
        }
      }
  }

  def createOrUpdateSurveyStaff(surveyId: String) = deadbolt.restrictAccess(Roles.superuser, Roles.surveyStaff(surveyId))(upickleBodyParser[CreateOrUpdateUsersRequest]) {
    request =>
      Future {
        doCreateOrUpdate(Some(surveyId), request.body.userRecords.map {
          record =>
            UserRecordWithPermissions(record.userName, record.password, record.name, record.email, record.phone, record.customFields, Set(Roles.surveyStaff(surveyId)), Set())
        })
      }
  }

  def createOrUpdateSurveyRespondents(surveyId: String) = deadbolt.restrictAccess(Roles.superuser, Roles.surveyStaff(surveyId))(upickleBodyParser[CreateOrUpdateUsersRequest]) {
    request =>
      Future {
        doCreateOrUpdate(Some(surveyId), request.body.userRecords.map {
          record =>
            UserRecordWithPermissions(record.userName, record.password, record.name, record.email, record.phone, record.customFields, Set(Roles.surveyRespondent(surveyId)), Set())
        })
      }
  }
}

