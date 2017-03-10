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
import be.objectify.deadbolt.scala.models.Subject
import com.mohiva.play.silhouette.api.Environment
import play.api.mvc.Request

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DeadboltAccessHandlerImpl(val env: Environment[Intake24ApiEnv]) extends AbstractDeadboltHandler {

  val dynamicHandler = new DynamicResourceHandler {
    override def checkPermission[A](permissionValue: String, meta: Option[Any], deadboltHandler: DeadboltHandler, request: AuthenticatedRequest[A]) = ???

    override def isAllowed[A](name: String, meta: Option[Any], deadboltHandler: DeadboltHandler, request: AuthenticatedRequest[A]) = {
      val allowed = name match {
        case "respondent" => request.subject match {
          case Some(subj) => subj.roles.exists(_.name.endsWith("respondent"))
          case None => false
        }
        case _ => throw new RuntimeException(s"Dynamic constraint $name not defined")
      }

      Future.successful(allowed)
    }
  }

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = {
    env.authenticatorService.retrieve(request).map(getAccessSubjectFromJWT)
  }

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] =
    Future.successful(Some(dynamicHandler))
}
