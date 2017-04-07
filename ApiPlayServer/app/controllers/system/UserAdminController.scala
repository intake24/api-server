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

import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import controllers.DatabaseErrorHandler
import io.circe.generic.auto._
import parsers.{JsonUtils, UserRecordsCSVParser}
import play.api.Logger
import play.api.http.ContentTypes
import play.api.libs.Files
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller, MultipartFormData, Result}
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.api.shared._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SurveyUser

import scala.concurrent.Future

class UserAdminController @Inject()(service: UserAdminService, passwordHasherRegistry: PasswordHasherRegistry, deadbolt: DeadboltActionsAdapter) extends Controller
  with DatabaseErrorHandler with JsonUtils {

  private def doCreateOrUpdate(surveyId: String, roles: Set[String], userRecords: Seq[SurveyUser]): Result = {
    val hasher = passwordHasherRegistry.current

    val newUserRecords = userRecords.map {
      record =>
        val passwordInfo = hasher.hash(record.password)

        NewUserWithAlias(
          SurveyUserAlias(surveyId, record.userName),
          UserInfo(record.name, record.email, record.phone, roles, record.customFields),
          SecurePassword(passwordInfo.password, passwordInfo.salt.get, passwordInfo.hasher))
    }

    translateDatabaseResult(service.createOrUpdateUsersWithAliases(newUserRecords))
  }

  private def uploadCSV(formData: MultipartFormData[Files.TemporaryFile], surveyId: String, roles: Set[String]): Result = {
    if (formData.files.length != 1)
      BadRequest(toJsonString(ErrorDescription("BadRequest", s"Expected exactly one file attachment, got ${formData.files.length}"))).as(ContentTypes.JSON)
    else {
      UserRecordsCSVParser.parseFile(formData.files(0).ref.file) match {
        case Right(csvRecords) =>
          doCreateOrUpdate(surveyId, roles, csvRecords)
        case Left(error) =>
          BadRequest(toJsonString(ErrorDescription("InvalidCSV", error)))
      }
    }
  }

  def findUsers(query: String, limit: Int) = deadbolt.restrictToRoles(Roles.superuser)(BodyParsers.parse.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.findUsers(query, Math.min(Math.max(limit, 0), 100)))
      }
  }

  def createUser() = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[CreateUserRequest]) {
    request =>
      Future {
        val pwInfo = passwordHasherRegistry.current.hash(request.body.password)

        translateDatabaseResult(service.createUser(NewUser(request.body.userInfo, SecurePassword(pwInfo.password, pwInfo.salt.get, pwInfo.hasher))))
      }
  }

  def deleteUsers() = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[DeleteUsersRequest]) {
    request =>
      Future {
        translateDatabaseResult(service.deleteUsersById(request.body.userIds))
      }
  }

  def listSurveyStaffUsers(surveyId: String, offset: Int, limit: Int) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyStaff(surveyId))(BodyParsers.parse.empty) {
    _ =>
      Future {
        translateDatabaseResult(service.listUsersByRole(Roles.surveyStaff(surveyId), offset, limit))
      }
  }

  def createOrUpdateSurveyStaff(surveyId: String) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyStaff(surveyId))(jsonBodyParser[CreateOrUpdateSurveyUsersRequest]) {
    request =>
      Future {
        doCreateOrUpdate(surveyId, Set(Roles.surveyStaff(surveyId)), request.body.users)
      }
  }

  def uploadSurveyStaffCSV(surveyId: String) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyStaff(surveyId))(BodyParsers.parse.multipartFormData) {
    request =>
      Future {
        uploadCSV(request.body, surveyId, Set(Roles.surveyStaff(surveyId)))
      }
  }

  /**
    * Only users that have a user name in this survey will be returned.
    *
    * If someone has a respondent role but does not have a user alias for this survey they will be filtered out.
    *
    * This is because client-side user presentation currently does not make sense without a user name.
    */
  def listSurveyRespondentUsers(surveyId: String, offset: Int, limit: Int) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyStaff(surveyId))(BodyParsers.parse.empty) {
    _ =>
      Future {
        val result =
          for (users <- service.listUsersByRole(Roles.surveyRespondent(surveyId), offset, limit).right;
               surveyUserNames <- service.getSurveyUserNames(users.map(_.id), surveyId).right)
            yield
              users.filter(u => surveyUserNames.contains(u.id)).map {
                user =>
                  UserInfoWithSurveyUserName(user.id, surveyUserNames(user.id), user.name, user.email, user.phone, user.roles, user.customFields)
              }

        translateDatabaseResult(result)
      }
  }

  def createOrUpdateSurveyRespondents(surveyId: String) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyStaff(surveyId))(jsonBodyParser[CreateOrUpdateSurveyUsersRequest]) {
    request =>
      Future {
        doCreateOrUpdate(surveyId, Set(Roles.surveyRespondent(surveyId)), request.body.users)
      }
  }

  def uploadSurveyRespondentsCSV(surveyId: String) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyStaff(surveyId))(BodyParsers.parse.multipartFormData) {
    request =>
      Future {
        uploadCSV(request.body, surveyId, Set(Roles.surveyRespondent(surveyId)))
      }
  }

  def deleteSurveyUsers(surveyId: String) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyStaff(surveyId))(jsonBodyParser[DeleteSurveyUsersRequest]) {
    request =>
      Future {
        translateDatabaseResult(service.deleteUsersByAlias(request.body.userNames.map(n => SurveyUserAlias(surveyId, n))))
      }
  }
}
