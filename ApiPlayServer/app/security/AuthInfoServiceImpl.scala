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

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordInfo
import play.api.Logger
import uk.ac.ncl.openlab.intake24.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserAdminService

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class AuthInfoServiceImpl @Inject()(userAdminService: UserAdminService,
                                    implicit val executionContext: ExecutionContext) extends AuthInfoRepository {

  def find[T](loginInfo: LoginInfo)(implicit tag: ClassTag[T]): Future[Option[T]] = Future {
    val databaseResult = loginInfo.providerID match {
      case SurveyAliasProvider.ID => userAdminService.getUserPasswordByAlias(SurveyAliasUtils.fromString(loginInfo.providerKey))
      case EmailProvider.ID => userAdminService.getUserPasswordByEmail(loginInfo.providerKey)
      case x => throw new RuntimeException(s"Auth info provider $x not supported")
    }

    databaseResult match {
      case Right(password) => Some(PasswordInfo(password.hasher, password.hashBase64, Some(password.saltBase64)).asInstanceOf[T])
      case Left(RecordNotFound(_)) => None
      case Left(e) => throw e.exception
    }
  }

  def save[T](loginInfo: LoginInfo, authInfo: T): Future[T] = ???

  def add[T](loginInfo: LoginInfo, authInfo: T): Future[T] = ???

  def remove[T](loginInfo: LoginInfo)(implicit tag: scala.reflect.ClassTag[T]): Future[Unit] = ???

  def update[T](loginInfo: LoginInfo, authInfo: T): scala.concurrent.Future[T] = ???
}