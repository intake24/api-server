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

case class MealTime(hours: Int, minutes: Int)

case class CompletedPortionSize(scriptName: String, data: Map[String, String])

case class NutritionMappedFood(code: String, englishDescription: String, localDescription: Option[String], nutrientTableID: String, nutrientTableCode: String, isReadyMeal: Boolean, searchTerm: String, portionSize: CompletedPortionSize,
  foodGroupCode: Int, foodGroupEnglishDescription: String, foodGroupLocalDescription: Option[String], reasonableAmount: Boolean, brand: String, nutrients: Map[Long, Double], customData: Map[String, String])

case class NutritionMappedMeal(name: String, foods: Seq[NutritionMappedFood], time: MealTime, customData: Map[String, String])

case class NutritionMappedSurvey(startTime: Long, endTime: Long, meals: Seq[NutritionMappedMeal], log: Seq[String], userName: String, customData: Map[String, String])

case class NutritionMappedSurveyRecord(survey: NutritionMappedSurvey, userCustomFields: Map[String, String])

case class NutritionMappedSurveyRecordWithId(id: String, survey: NutritionMappedSurvey, userCustomFields: Map[String, String])

case class MissingFoodRecord(submittedAt: Long, surveyId: String, userName: String, name: String, brand: String, description: String, portionSize: String, leftovers: String)

case class SurveyParameters(state: Int, startDate: Long, endDate: Long, schemeName: String, locale: String, allowGenUsers: Boolean, supportEmail: String, suspensionReason: String, surveyMonkeyUrl: Option[String])

case class SupportUserRecord(surveyId: String, userName: String, realName: Option[String], email: Option[String], phoneNumber: Option[String], smsEnabled: Boolean)

case class LocalNutrientType(nutrientTypeId: Long, localDescription: String, unit: String)