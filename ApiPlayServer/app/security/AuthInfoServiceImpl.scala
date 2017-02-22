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

import com.mohiva.play.silhouette.api.AuthInfo
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import scala.reflect.ClassTag
import scala.concurrent.Future
import play.api.Logger

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger
import javax.inject.Inject
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import javax.inject.Singleton
import com.mohiva.play.silhouette.api.util.PasswordInfo
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreScala

@Singleton
class AuthInfoServiceImpl @Inject() (dataStore: DataStoreScala) extends AuthInfoRepository {

  def find[T](loginInfo: LoginInfo)(implicit tag: ClassTag[T]): Future[Option[T]] = Future {

    Logger.debug("Retrieving " + loginInfo.toString())

    val intake24key = Intake24UserKey.fromString(loginInfo.providerKey)

    dataStore.getUserRecord(intake24key.surveyId.getOrElse(""), intake24key.userName).map {
      record =>
        PasswordInfo(record.passwordHasher, record.passwordHashBase64, Some(record.passwordSaltBase64)).asInstanceOf[T]
    }
  }

  def save[T](loginInfo: LoginInfo, authInfo: T): Future[T] = ???
  def add[T](loginInfo: LoginInfo, authInfo: T): Future[T] = ???
  def remove[T](loginInfo: LoginInfo)(implicit tag: scala.reflect.ClassTag[T]): Future[Unit] = ???

  def update[T](loginInfo: LoginInfo, authInfo: T): scala.concurrent.Future[T] = ???
  /* 
   
   Updating user records is not currently supported 
    
   Future {
    Logger.debug("Updating " + loginInfo.toString())
    
    val info = authInfo.asInstanceOf[PasswordInfo]

    val intake24key = Intake24UserKey.fromString(loginInfo.providerKey)
    
    dataStore.getUserRecord(intake24key.surveyName, intake24key.userName) match {
      case Some(record) => {
        val newRecord = SecureUserRecord(record.username, info.password, info.salt.get, info.hasher, record.roles, record.permissions, record.customFields)
        dataStore.saveUsers(intake24key.surveyName, Seq(newRecord))
        authInfo
      }
      case None => throw new RuntimeException("User record not found")
    }
  }*/
}