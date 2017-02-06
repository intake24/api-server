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
import parsers.UpickleUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SecureUserRecord, UserAdminService}
import upickle.default._

import scala.concurrent.Future

case class UserRef(surveyId: String, id: String)

case class SupportUsersUpdateRequest(users: Seq[UserRef])

class SurveyAdminController @Inject()(service: UserAdminService, passwordHasherRegistry: PasswordHasherRegistry, deadbolt: DeadboltActionsAdapter) extends Controller
  with SystemDatabaseErrorHandler with UpickleUtil {

  def updateGlobalSupportUsers() = deadbolt.restrictAccess(Roles.superuser)(upickleBodyParser[SupportUsersUpdateRequest]) {
    request =>
      Future {
        ???
        // translateDatabaseResult(service.createOrUpdateUsers(request.body.surveyId, secureUserRecords))
      }
  }
}
