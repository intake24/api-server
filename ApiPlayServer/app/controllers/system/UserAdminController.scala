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
import play.api.mvc.{BodyParsers, Controller}
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SecureUserRecord, UserAdminService}
import upickle.default._

import scala.collection.JavaConverters._
import scala.concurrent.Future

case class UserRecord(userName: String, password: String, name: Option[String], email: Option[String], phone: Option[String], customFields: Map[String, String], roles: Set[String], permissions: Set[String])

case class CreateOrUpdateGlobalUsersRequest(userRecords: Seq[UserRecord])

case class ShortUserRecord(userName: String, password: String, name: Option[String], email: Option[String], phone: Option[String], customFields: Map[String, String])

class UserAdminController @Inject()(service: UserAdminService, passwordHasherRegistry: PasswordHasherRegistry, deadbolt: DeadboltActionsAdapter) extends Controller
  with SystemDatabaseErrorHandler with UpickleUtil {

  private def jopt2option[T](option: org.workcraft.gwt.shared.client.Option[T]) = if (option.isEmpty()) None else Some(option.getOrDie)

  private def fromJavaUserRecords(records: java.util.List[net.scran24.datastore.shared.UserRecord]): Seq[ShortUserRecord] =
    records.asScala.toList.map {
      record =>
        ShortUserRecord(record.username, record.password, jopt2option(record.name), jopt2option(record.email), jopt2option(record.phone), record.customFields.asScala.toMap)
    }

  def createOrUpdateGlobalUsers() = deadbolt.restrictAccess(Roles.superuser)(upickleBodyParser[CreateOrUpdateGlobalUsersRequest]) {
    request =>
      Future {
        val hasher = passwordHasherRegistry.current

        val secureUserRecords = request.body.userRecords.map {
          record =>
            val passwordInfo = hasher.hash(record.password)
            SecureUserRecord(record.userName, passwordInfo.password, passwordInfo.salt.get, passwordInfo.hasher, record.name, record.email, record.phone, record.roles, record.permissions, record.customFields)
        }

        translateDatabaseResult(service.createOrUpdateUsers(None, secureUserRecords))
      }
  }

  /*def uploadUsersCSV() = deadbolt.restrictAccess(Roles.superuser)(BodyParsers.parse.multipartFormData) {
    request =>
      Future {
        val formData = request.body

        if (formData.files.length != 1)
          BadRequest(write(ErrorDescription("BadRequest", s"Expected exactly one file attachment, got ${
            formData.files.length
          }"))).as(ContentTypes.JSON)
        else {
          val csvFile = formData.files(0).ref.file

          val is = new FileInputStream(csvFile)

          try {
            UserRecordCSV.fromCSV(is)
          } catch {
            case e: Throwable => BadRequest(write(ErrorDescription("CsvParseError", s"Failed to parse user records: ${
              e.getClass.getSimpleName
            }: ${
              e.getMessage
            }")))
          } finally {
            is.close()
          }


        }
      }*/
  }
