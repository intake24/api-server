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

import be.objectify.deadbolt.scala.DeadboltHandler
import play.api.mvc.Request
import play.api.mvc.Results
import scala.concurrent.Future
import models.User
import be.objectify.deadbolt.scala.DynamicResourceHandler
import scala.concurrent.ExecutionContext.Implicits.global
import be.objectify.deadbolt.core.models.Subject
import javax.inject.Inject
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import play.api.mvc.Result

import upickle.default._
import play.api.libs.json._
import play.Logger
import models.SecurityInfo

class DeadboltHandlerImpl(val env: Environment[User, JWTAuthenticator]) extends DeadboltHandler {

  def beforeAuthCheck[A](request: Request[A]) = Future(None)

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = Future(None)

  override def getSubject[A](request: Request[A]): Future[Option[Subject]] = {
    env.authenticatorService.retrieve(request).map(_.flatMap {
      authenticator =>
        authenticator.customClaims.map {
          customClaims =>
            val roles = (customClaims \ "i24r").get.as[Set[String]]
            val permissions = (customClaims \ "i24p").get.as[Set[String]]
            new User(authenticator.loginInfo.providerKey, SecurityInfo(roles, permissions))
        }
    })
  }

  def onAuthFailure[A](request: Request[A]): Future[Result] =
    env.authenticatorService.retrieve(request).map {
    case Some(_) => Results.Forbidden
    case None => Results.Unauthorized.withHeaders(("WWW-Authenticate", "X-Auth-Token"))
  }
    
}
