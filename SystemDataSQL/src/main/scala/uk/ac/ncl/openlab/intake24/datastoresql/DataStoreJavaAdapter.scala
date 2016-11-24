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

import net.scran24.datastore.DataStore
import JavaConversions._
import scala.collection.JavaConversions._
import org.workcraft.gwt.shared.client.{ Option => JOpt }
import java.util.{ List => JList, Map => JMap, Set => JSet }
import net.scran24.datastore.{
  SecureUserRecord => JavaSecureUserRecord,
  NutritionMappedFood => JavaNutritionMappedFood,
  NutritionMappedMeal => JavaNutritionMappedMeal,
  NutritionMappedSurvey => JavaNutritionMappedSurvey,
  NutritionMappedSurveyRecord => JavaNutritionMappedSurveyRecord,
  NutritionMappedSurveyRecordWithId => JavaNutritionMappedSurveyRecordWithId,
  MissingFoodRecord => JavaMissingFoodRecord,
  SupportStaffRecord => JavaSupportStaffRecord
}
import net.scran24.datastore.shared.{ Time => JavaMealTime, CompletedPortionSize => JavaCompletedPortionSize, SurveyParameters => JavaSurveyParameters }
import net.scran24.datastore.shared.SurveyState
import org.workcraft.gwt.shared.client.Callback1
import com.google.inject.Singleton
import com.google.inject.Inject

@Singleton
class DataStoreJavaAdapter @Inject() (scalaImpl: DataStoreScala) extends DataStore {

  // This is a nasty hack to make SQL datastore compatible with MongoDB data store
  // that uses a special survey name 'admin' for users not belonging to a survey
  // SqlDataStore uses an empty survey name instead

  @deprecated("Reconcile MongoDB and SQL implementation regarding special users")
  def overrideAdminSurveyId(survey_id: String) = survey_id match {
    case "admin" => ""
    case other => other
  }

  def initSurvey(survey_id: String, scheme_name: String, locale: String, allowGenUsers: Boolean, surveyMonkeyUrl: JOpt[String]): Unit =
    scalaImpl.initSurvey(survey_id, scheme_name, locale, allowGenUsers, jopt2option(surveyMonkeyUrl))

  def getUserData(survey_id: String, user_id: String): JMap[String, String] = {
    copyToJavaMap(scalaImpl.getUserData(overrideAdminSurveyId(survey_id), user_id))
  }

  def setUserData(survey_id: String, user_id: String, userData: JMap[String, String]): Unit =
    scalaImpl.setUserData(overrideAdminSurveyId(survey_id), user_id, userData.toMap)

  def saveUsers(survey_id: String, users: JList[JavaSecureUserRecord]): Unit =
    scalaImpl.saveUsers(survey_id, users.toSeq.map(fromJavaSecureUserRecord))

  def addUser(survey_id: String, user: JavaSecureUserRecord): Unit =
    scalaImpl.addUser(overrideAdminSurveyId(survey_id), fromJavaSecureUserRecord(user))

  def getUserRecord(survey_id: String, username: String): JOpt[JavaSecureUserRecord] =
    option2jopt(scalaImpl.getUserRecord(overrideAdminSurveyId(survey_id), username).map(toJavaSecureUserRecord))

  def getUserRecords(survey_id: String, role: String): JList[JavaSecureUserRecord] =
    copyToJavaList(scalaImpl.getUserRecords(overrideAdminSurveyId(survey_id), role).map(toJavaSecureUserRecord))

  def getUserRecords(survey_id: String): JList[JavaSecureUserRecord] =
    copyToJavaList(scalaImpl.getUserRecords(overrideAdminSurveyId(survey_id)).map(toJavaSecureUserRecord))

  def saveSurvey(survey_id: String, username: String, survey: JavaNutritionMappedSurveyRecord): Unit =
    scalaImpl.saveSurvey(survey_id, username, fromJavaNutritionMappedSurveyRecord(survey))

  def saveMissingFoods(missingFoods: JList[JavaMissingFoodRecord]): Unit =
    scalaImpl.saveMissingFoods(missingFoods.toSeq.map(fromJavaMissingFoodRecord))

  def processMissingFoods(timeFrom: Long, timeTo: Long, processMissingFood: Callback1[JavaMissingFoodRecord]): Unit =
    scalaImpl.processMissingFoods(timeFrom, timeTo, r => processMissingFood.call(toJavaMissingFoodRecord(r)))

  def processSurveys(survey_id: String, timeFrom: Long, timeTo: Long, processSurvey: Callback1[JavaNutritionMappedSurveyRecordWithId]): Unit =
    scalaImpl.processSurveys(survey_id, timeFrom, timeTo, r => processSurvey.call(toJavaNutritionMappedSurveyRecordWithId(r)))

  def getSurveyParameters(survey_id: String): JavaSurveyParameters =
    toJavaSurveyParameters(scalaImpl.getSurveyParameters(survey_id))

  def setSurveyParameters(survey_id: String, newParameters: JavaSurveyParameters): Unit =
    scalaImpl.setSurveyParameters(survey_id, fromJavaSurveyParameters(newParameters))

  def getPopularityCount(foodCodes: JSet[String]): JMap[String, Integer] =
    copyToJavaMap(scalaImpl.getPopularityCount(foodCodes.toSeq), (int: Int) => new java.lang.Integer(int))

  def incrementPopularityCount(foodCodes: JList[String]): Unit =
    scalaImpl.incrementPopularityCount(foodCodes.toSeq)

  def setGlobalValue(name: String, value: String): Unit =
    scalaImpl.setGlobalValue(name, value)

  def getGlobalValue(name: String): JOpt[String] =
    option2jopt(scalaImpl.getGlobalValue(name))

  def getSupportStaffRecords(): JList[JavaSupportStaffRecord] =
    copyToJavaList(scalaImpl.getSupportStaffRecords().map(toJavaSupportStaffRecord))

  def getLastHelpRequestTime(survey: String, username: String): JOpt[java.lang.Long] =
    option2jopt(scalaImpl.getLastHelpRequestTime(survey, username).map(new java.lang.Long(_)))

  def setLastHelpRequestTime(survey: String, username: String, time: Long): Unit =
    scalaImpl.setLastHelpRequestTime(survey, username, time)

  def generateCompletionCode(survey: String, username: String, external_user_id: String): String = 
    scalaImpl.generateCompletionCode(survey, username, external_user_id)    

  def validateCompletionCode(survey: String, external_user_id: String, code: String): Boolean = 
    scalaImpl.validateCompletionCode(survey, external_user_id, code)

  def getSurveyNames(): JList[String] = {
    copyToJavaList(scalaImpl.getSurveyNames())
  }
  
}