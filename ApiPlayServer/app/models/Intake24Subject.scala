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

package models

import be.objectify.deadbolt.scala.models.Subject
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator

sealed trait Intake24Subject extends Subject {
  val userId: Long
  val jwt: JWTAuthenticator
}

case class RefreshSubject(identifier: String, userId: Long, jwt: JWTAuthenticator) extends Intake24Subject {
  val roles = List()
  val permissions = List()
}

case class AccessSubject(identifier: String, userId: Long, userRoles: Set[String], jwt: JWTAuthenticator) extends Intake24Subject {
  def roles = userRoles.toList.map(SecurityRole(_))

  val permissions = List()
}