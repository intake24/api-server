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
import uk.ac.ncl.openlab.intake24.api.shared.{SurveyAliasCredentials => Intake24Credentials}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SurveyUserAlias

import scala.language.implicitConversions

object SurveyAliasUtils {
  private val separatorChar = '#'

  def toString(alias: SurveyUserAlias) = alias.surveyId + separatorChar + alias.userName

  def fromString(s: String) = {
    val separatorIndex = s.lastIndexOf(separatorChar)

    if (separatorIndex == -1)
      throw new RuntimeException("surveyId is required")

    SurveyUserAlias(s.substring(0, separatorIndex), s.substring(separatorIndex + 1))
  }
}

object Intake24CredentialsUtil {
  implicit def asSimpleCredentials(credentials: Intake24Credentials) = Credentials(SurveyUserAlias(credentials.surveyId, credentials.userName).toString(), credentials.password)

  implicit def fromSimpleCredentials(credentials: Credentials) = {
    val key = SurveyAliasUtils.fromString(credentials.identifier)

    Intake24Credentials(key.surveyId, key.userName, credentials.password)
  }

  implicit val jsonFormat = Json.format[Intake24Credentials]
}