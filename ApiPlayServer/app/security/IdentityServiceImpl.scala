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

package security

import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.LoginInfo
import scala.concurrent.Future
import _root_.models.User
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger
import models.SecurityInfo
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreScala

class IdentityServiceImpl @Inject() (val dataStore: DataStoreScala) extends IdentityService[User] {

  implicit val securityInfoFormat = Json.format[SecurityInfo]

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = Future {
    val intake24key = Intake24UserKey.fromString(loginInfo.providerKey)

    dataStore.getUserRecord(intake24key.surveyName, intake24key.userName).map {
      record =>
        User(record.username, SecurityInfo(record.roles, record.permissions))
    }
  }

}