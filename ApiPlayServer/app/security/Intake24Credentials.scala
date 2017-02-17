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

import play.api.libs.json.Json
import com.mohiva.play.silhouette.api.util.Credentials
import uk.ac.ncl.openlab.intake24.api.shared.{ Credentials => Intake24Credentials }

case class Intake24UserKey(userName: String, surveyName: String) {
  override def toString = userName + Intake24UserKey.separatorChar + surveyName
}

object Intake24UserKey {  
  private val separatorChar = '#'
  
  def fromString(s: String) = {
    val separatorIndex = s.lastIndexOf(separatorChar)
    if (separatorIndex == -1)
      Intake24UserKey(s, "")
    else
      Intake24UserKey(s.substring(0, separatorIndex), s.substring(separatorIndex + 1))
  } 
}

object Intake24CredentialsUtil {
  implicit def asSimpleCredentials(credentials: Intake24Credentials) = Credentials(Intake24UserKey(credentials.username, credentials.survey_id.getOrElse("")).toString(), credentials.password)
  
  implicit def fromSimpleCredentials(credentials: Credentials) = {
    val key = Intake24UserKey.fromString(credentials.identifier)
    val surveyId = if (key.surveyName.isEmpty) None else Some(key.surveyName)

    Intake24Credentials(surveyId, key.userName, credentials.password)
  } 
  
  implicit val jsonFormat = Json.format[Intake24Credentials]  
}