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

import javax.inject.Inject

import _root_.models.Intake24User
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.ac.ncl.openlab.intake24.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserAdminService

import scala.concurrent.Future

class IdentityServiceImpl @Inject()(val userAdminService: UserAdminService) extends IdentityService[Intake24User] {

  def retrieve(loginInfo: LoginInfo): Future[Option[Intake24User]] = Future {

    val databaseResult = loginInfo.providerID match {
      case SurveyAliasProvider.ID => userAdminService.getUserByAlias(SurveyAliasUtils.fromString(loginInfo.providerKey))
      case EmailProvider.ID => userAdminService.getUserByEmail(loginInfo.providerKey)
      case URLTokenProvider.ID => userAdminService.getUserByUrlToken(loginInfo.providerKey)
      case x => throw new RuntimeException(s"Unsupported login provider: $x")
    }

    databaseResult match {
      case Right(userInfo) => Some(Intake24User(loginInfo, userInfo))
      case Left(RecordNotFound(_)) => None
      case Left(e) => throw e.exception
    }
  }
}
