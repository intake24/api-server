package uk.ac.ncl.openlab.intake24.surveydata

import java.time.Instant
import java.util.UUID

case class NutrientMappedSubmission(id: UUID, userName: String, userCustomData: Map[String, String], surveyCustomData: Map[String, String], startTime: Instant, endTime: Instant, meals: Seq[NutrientMappedMeal])

case class MealTime(hours: Int, minutes: Int)

case class NutrientMappedMeal(name: String, time: MealTime, customData: Map[String, String], foods: Seq[NutrientMappedFood])

case class NutrientMappedFood(code: String, englishDescription: String, localDescription: Option[String], searchTerm: String, nutrientTableId: String, nutrientTableCode: String, isReadyMeal: Boolean,
                              portionSize: CompletedPortionSize, reasonableAmount: Boolean, foodGroupId: Int, brand: String, nutrients: Seq[(Int, Double)], customData: Map[String, String])

case class CompletedPortionSize(method: String, data: Map[String, String])