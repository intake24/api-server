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

package uk.ac.ncl.openlab.intake24.datastoresql

import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SecureUserRecord

trait DataStoreScala {

  def initSurvey(survey_id: String, scheme_name: String, locale: String, allowGenUsers: Boolean, surveyMonkeyUrl: Option[String], supportEmail: String): Unit
  
  def getSurveyNames(): Seq[String]

  def getUserData(survey_id: String, user_id: String): Map[String, String]

  def setUserData(survey_id: String, user_id: String, userData: Map[String, String]): Unit

  def saveUsers(survey_id: String, users: Seq[SecureUserRecord]): Unit
  
  def addUser(survey_id: String, user: SecureUserRecord): Unit

  def getUserRecord(survey_id: String, username: String): Option[SecureUserRecord]

  def getUserRecords(survey_id: String, role: String): Seq[SecureUserRecord]

  def getUserRecords(survey_id: String): Seq[SecureUserRecord]

  def saveSurvey(survey_id: String, username: String, survey: NutritionMappedSurveyRecord): Unit

  def saveMissingFoods(missingFoods: Seq[MissingFoodRecord]): Unit

  def processMissingFoods(timeFrom: Long, timeTo: Long, processMissingFood: MissingFoodRecord => Unit): Unit

  def processSurveys(survey_id: String, timeFrom: Long, timeTo: Long, processSurvey: NutritionMappedSurveyRecordWithId => Unit): Unit

  def getSurveyParameters(survey_id: String): SurveyParameters

  def setSurveyParameters(survey_id: String, newParameters: SurveyParameters): Unit

  def getPopularityCount(foodCodes: Seq[String]): Map[String, Int]

  def incrementPopularityCount(foodCodes: Seq[String]): Unit

  def setGlobalValue(name: String, value: String): Unit

  def getGlobalValue(name: String): Option[String]

  def getSupportUserRecords(surveyId: String): Seq[SupportUserRecord]

  def getLastHelpRequestTime(survey: String, username: String): Option[Long]

  def setLastHelpRequestTime(survey: String, username: String, time: Long): Unit

  def generateCompletionCode(survey: String, username: String, external_user_id: String): String

  def validateCompletionCode(survey: String, external_user_id: String, code: String): Boolean

  def getLocalNutrientTypes(locale_id: String): Seq[LocalNutrientType]
  
  def getSurveySupportEmail(surveyId: String): String
}
