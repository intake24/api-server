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

import com.mohiva.play.silhouette.api.Identity
import com.mohiva.play.silhouette.api.LoginInfo
import be.objectify.deadbolt.core.models.Subject
import play.libs.Scala

case class User(id: String, securityInfo: SecurityInfo) extends Identity with Subject {    
  
  def getRoles: java.util.List[SecurityRole] = Scala.asJava(securityInfo.roles.map(SecurityRole).toSeq)

  def getPermissions: java.util.List[SecurityPermission] =  Scala.asJava((securityInfo.permissions).map(SecurityPermission).toSeq)
  
  def getIdentifier() = id
}
