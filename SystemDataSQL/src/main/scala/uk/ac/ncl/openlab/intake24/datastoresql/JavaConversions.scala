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

import net.scran24.datastore.{
SecureUserRecord => JavaSecureUserRecord,
NutritionMappedFood => JavaNutritionMappedFood,
NutritionMappedMeal => JavaNutritionMappedMeal,
NutritionMappedSurvey => JavaNutritionMappedSurvey,
NutritionMappedSurveyRecord => JavaNutritionMappedSurveyRecord,
NutritionMappedSurveyRecordWithId => JavaNutritionMappedSurveyRecordWithId,
MissingFoodRecord => JavaMissingFoodRecord,
SupportUserRecord => JavaSupportUserRecord
}
import scala.collection.JavaConversions._
import net.scran24.datastore.shared.{Time => JavaMealTime, CompletedPortionSize => JavaCompletedPortionSize, SurveyParameters => JavaSurveyParameters}
import net.scran24.datastore.shared.SurveyState
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SecureUserRecord

// This code handles the conversion between Scala and Java datastore types.
// It is required because automatic wrapping of Scala types with Java interfaces breaks 
// GWT reflection-based serialisation mechanism, so instead of wrapping Scala collections 
// data has instead to be copied to fresh Java collections that GWT understands.
//
// Explicit conversions between Java / Scala basic types that are not shared (e.g. Long, Double) 
// are also handled here to keep Scala code clean.
object JavaConversions {

  def jopt2option[T](option: org.workcraft.gwt.shared.client.Option[T]) = if (option.isEmpty()) None else Some(option.getOrDie)

  def option2jopt[T](option: Option[T]) = option match {
    case Some(value) => org.workcraft.gwt.shared.client.Option.some(value)
    case None => org.workcraft.gwt.shared.client.Option.none[T]()
  }

  def copyToJavaMap[K, V](map: Map[K, V]): java.util.Map[K, V] = new java.util.HashMap(map)

  def copyToJavaMap[K, V, JV](map: Map[K, V], conv: V => JV): java.util.Map[K, JV] = new java.util.HashMap(map.mapValues(conv))

  def copyToJavaSet[T](set: Set[T]): java.util.Set[T] = new java.util.HashSet[T](set)

  def copyToJavaList[T](seq: Seq[T]): java.util.List[T] = new java.util.ArrayList[T](seq)

  def toJavaSecureUserRecord(record: SecureUserRecord): JavaSecureUserRecord =
    new JavaSecureUserRecord(record.userName, record.passwordHashBase64, record.passwordSaltBase64, record.passwordHasher,
      option2jopt(record.name), option2jopt(record.email), option2jopt(record.phone),
      copyToJavaSet(record.roles), copyToJavaSet(record.permissions), copyToJavaMap(record.customFields))

  def fromJavaSecureUserRecord(record: JavaSecureUserRecord): SecureUserRecord =
    SecureUserRecord(record.username, record.passwordHashBase64, record.passwordSaltBase64, record.passwordHasher,
      jopt2option(record.name), jopt2option(record.email), jopt2option(record.phone),
      record.roles.toSet, record.permissions.toSet, record.customFields.toMap)

  def toJavaMealTime(time: MealTime): JavaMealTime = new JavaMealTime(time.hours, time.minutes)

  def fromJavaMealTime(time: JavaMealTime): MealTime = MealTime(time.hours, time.minutes)

  def toJavaCompletedPortionSize(ps: CompletedPortionSize): JavaCompletedPortionSize =
    new JavaCompletedPortionSize(ps.scriptName, copyToJavaMap(ps.data))

  def fromJavaCompletedPortionSize(ps: JavaCompletedPortionSize): CompletedPortionSize =
    CompletedPortionSize(ps.scriptName, ps.data.toMap)

  def toJavaNutritionMappedFood(food: NutritionMappedFood): JavaNutritionMappedFood =
    new JavaNutritionMappedFood(food.code, food.englishDescription, option2jopt(food.localDescription), food.nutrientTableID, food.nutrientTableCode, food.isReadyMeal, food.searchTerm, toJavaCompletedPortionSize(food.portionSize),
      food.foodGroupCode, food.foodGroupEnglishDescription, option2jopt(food.foodGroupLocalDescription), food.reasonableAmount, food.brand, copyToJavaMap(food.nutrients.map { case (k, v) => new java.lang.Long(k) -> new java.lang.Double(v) }.toMap),
      copyToJavaMap(food.customData))

  def fromJavaNutritionMappedFood(food: JavaNutritionMappedFood): NutritionMappedFood =
    NutritionMappedFood(food.code, food.englishDescription, jopt2option(food.localDescription), food.nutrientTableID, food.nutrientTableCode, food.isReadyMeal, food.searchTerm, fromJavaCompletedPortionSize(food.portionSize),
      food.foodGroupCode, food.foodGroupEnglishDescription, jopt2option(food.foodGroupLocalDescription), food.reasonableAmount, food.brand, food.nutrients.toMap.map { case (k, v) => Long2long(k) -> Double2double(v) }, food.customData.toMap)

  def toJavaNutritionMappedMeal(meal: NutritionMappedMeal): JavaNutritionMappedMeal =
    new JavaNutritionMappedMeal(meal.name, copyToJavaList(meal.foods.map(toJavaNutritionMappedFood)), toJavaMealTime(meal.time), copyToJavaMap(meal.customData))

  def fromJavaNutritionMappedMeal(meal: JavaNutritionMappedMeal): NutritionMappedMeal =
    NutritionMappedMeal(meal.name, meal.foods.toSeq.map(fromJavaNutritionMappedFood), fromJavaMealTime(meal.time), meal.customData.toMap)

  def toJavaNutritionMappedSurvey(survey: NutritionMappedSurvey): JavaNutritionMappedSurvey =
    new JavaNutritionMappedSurvey(survey.startTime, survey.endTime, copyToJavaList(survey.meals.map(toJavaNutritionMappedMeal)), copyToJavaList(survey.log), survey.userName,
      copyToJavaMap(survey.customData))

  def fromJavaNutritionMappedSurvey(survey: JavaNutritionMappedSurvey): NutritionMappedSurvey =
    NutritionMappedSurvey(survey.startTime, survey.endTime, survey.meals.toSeq.map(fromJavaNutritionMappedMeal), survey.log.toSeq, survey.userName,
      survey.customData.toMap)

  def toJavaNutritionMappedSurveyRecord(record: NutritionMappedSurveyRecord): JavaNutritionMappedSurveyRecord =
    new JavaNutritionMappedSurveyRecord(toJavaNutritionMappedSurvey(record.survey), copyToJavaMap(record.userCustomFields))

  def fromJavaNutritionMappedSurveyRecord(record: JavaNutritionMappedSurveyRecord): NutritionMappedSurveyRecord =
    NutritionMappedSurveyRecord(fromJavaNutritionMappedSurvey(record.survey), record.userCustomFields.toMap)

  def toJavaNutritionMappedSurveyRecordWithId(record: NutritionMappedSurveyRecordWithId): JavaNutritionMappedSurveyRecordWithId =
    new JavaNutritionMappedSurveyRecordWithId(toJavaNutritionMappedSurvey(record.survey), copyToJavaMap(record.userCustomFields), record.id)

  def fromJavaNutritionMappedSurveyRecordWithId(record: JavaNutritionMappedSurveyRecordWithId): NutritionMappedSurveyRecordWithId =
    NutritionMappedSurveyRecordWithId(record.id, fromJavaNutritionMappedSurvey(record.survey), record.userCustomFields.toMap)

  def toJavaMissingFoodRecord(record: MissingFoodRecord): JavaMissingFoodRecord =
    new JavaMissingFoodRecord(record.submittedAt, record.surveyId, record.userName, record.name, record.brand, record.description, record.portionSize, record.leftovers)

  def fromJavaMissingFoodRecord(record: JavaMissingFoodRecord): MissingFoodRecord =
    MissingFoodRecord(record.submittedAt, record.surveyId, record.userName, record.name, record.brand, record.description, record.portionSize, record.leftovers)

  def toJavaSurveyParameters(params: SurveyParameters): JavaSurveyParameters =
    new JavaSurveyParameters(SurveyState.values()(params.state), params.startDate, params.endDate, params.schemeName, params.locale, params.allowGenUsers,
      params.supportEmail, params.suspensionReason, option2jopt(params.surveyMonkeyUrl))

  def fromJavaSurveyParameters(params: JavaSurveyParameters): SurveyParameters =
    SurveyParameters(params.state.ordinal(), params.startDate, params.endDate, params.schemeName, params.locale, params.allowGenUsers, params.supportEmail,
      params.suspensionReason, jopt2option(params.surveyMonkeyUrl))

  def toJavaSupportUserRecord(record: SupportUserRecord): JavaSupportUserRecord =
    new JavaSupportUserRecord(record.surveyId, record.userName, option2jopt(record.realName), option2jopt(record.email), option2jopt(record.phoneNumber), record.smsEnabled)

  def fromJavaSupportUserRecord(record: JavaSupportUserRecord): SupportUserRecord =
    SupportUserRecord(record.surveyId, record.userId, jopt2option(record.realName), jopt2option(record.email), jopt2option(record.phoneNumber), record.smsNotificationsEnabled)

}