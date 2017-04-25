package uk.ac.ncl.openlab.intake24.surveydata

import java.time.ZonedDateTime

case class MealTime(hours: Int, minutes: Int)

case class PortionSize(method: String, data: Map[String, String]) {
  def servingWeight = data("servingWeight").toDouble

  def leftoversWeight = data("leftoversWeight").toDouble

  def portionWeight = servingWeight - leftoversWeight
}

case class Food(code: String, isReadyMeal: Boolean, searchTerm: String, brand: String, portionSize: PortionSize, customData: Map[String, String])

case class Meal(name: String, foods: Seq[Food], missingFoods: Seq[MissingFood], time: MealTime, customData: Map[String, String])

case class MissingFood(name: String, brand: String, description: String, portionSize: String, leftovers: String)

case class SurveySubmission(startTime: ZonedDateTime, endTime: ZonedDateTime, meals: Seq[Meal], log: Seq[String], customData: Map[String, String])

case class NutrientMappedMeal(name: String, time: MealTime, customData: Map[String, String], foods: Seq[NutrientMappedFood], missingFoods: Seq[MissingFood])

case class NutrientMappedFood(code: String, englishDescription: String, localDescription: String, isReadyMeal: Boolean, searchTerm: String, brand: String, portionSize: PortionSize, customData: Map[String, String],
                              nutrientTableId: Option[String], nutrientTableCode: Option[String], reasonableAmount: Boolean, foodGroupId: Int, foodGroupEnglishDescription: String, foodGroupLocalDescription: Option[String], nutrients: Map[Long, Double])

case class NutrientMappedSubmission(startTime: ZonedDateTime, endTime: ZonedDateTime, meals: Seq[NutrientMappedMeal], log: Seq[String], customData: Map[String, String])
