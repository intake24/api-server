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

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import com.mohiva.play.silhouette.api.Environment
import play.api.mvc.{Request, Result, Results}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


abstract class AbstractDeadboltHandler extends DeadboltHandler with JWTHandlerUtil {

  override def beforeAuthCheck[A](request: Request[A]) = Future(None)

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = Future(None)

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = {
    val result = if (request.subject.isDefined) Results.Forbidden else Results.Unauthorized.withHeaders(("WWW-Authenticate", "X-Auth-Token"))
    Future.successful(result)
  }
}
