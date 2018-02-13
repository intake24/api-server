package uk.ac.ncl.openlab.intake24.surveydata

import java.time.ZonedDateTime
import java.util.UUID

case class MealTime(hours: Int, minutes: Int)

case class PortionSize(method: String, data: Map[String, String]) {
  def asPortionSizeWithWeights = {

    val servingWeight = data("servingWeight").toDouble

    val leftoversWeight = data.get("leftoversWeight").map(_.toDouble).getOrElse(0.0)

    PortionSizeWithWeights(servingWeight, leftoversWeight, Math.max(0, servingWeight - leftoversWeight), method, data)
  }
}

case class PortionSizeWithWeights(servingWeight: Double, leftoversWeight: Double, portionWeight: Double, method: String, data: Map[String, String])

case class Food(code: String, isReadyMeal: Boolean, searchTerm: String, brand: String, portionSize: PortionSize, customData: Map[String, String])

case class Meal(name: String, foods: Seq[Food], missingFoods: Seq[MissingFood], time: MealTime, customData: Map[String, String])

case class MissingFood(name: String, brand: String, description: String, portionSize: String, leftovers: String)

case class SurveySubmission(startTime: ZonedDateTime, endTime: ZonedDateTime, uxSessionId: UUID, meals: Seq[Meal], customData: Map[String, String])

case class NutrientMappedMeal(name: String, time: MealTime, customData: Map[String, String], foods: Seq[NutrientMappedFood], missingFoods: Seq[MissingFood])

case class NutrientMappedFood(code: String, englishDescription: String, localDescription: String, isReadyMeal: Boolean, searchTerm: String, brand: String, portionSize: PortionSizeWithWeights, customData: Map[String, String],
                              nutrientTableId: Option[String], nutrientTableCode: Option[String], reasonableAmount: Boolean, foodGroupId: Int, foodGroupEnglishDescription: String, foodGroupLocalDescription: Option[String], nutrients: Map[Long, Double])

case class NutrientMappedSubmission(startTime: ZonedDateTime, endTime: ZonedDateTime, uxSessionId: UUID, meals: Seq[NutrientMappedMeal], customData: Map[String, String])

case class SubmissionNotification(userId: Long, surveyId: String, userName: String, userCustomData: Map[String, String], startTime: ZonedDateTime, endTime: ZonedDateTime,
                                  uxSessionId: UUID, submissionId: UUID, submissionCustomData: Map[String, String], meals: Seq[NutrientMappedMeal])
