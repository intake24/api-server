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

package uk.ac.ncl.openlab.intake24.services.nutrition

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{FoodDataService, ResolvedFoodData}
import uk.ac.ncl.openlab.intake24.surveydata.{NutrientMappedFood, NutrientMappedMeal, NutrientMappedSubmission, SurveySubmission}

@Singleton
class DefaultNutrientMappingServiceImpl @Inject()(foodDataService: FoodDataService,
                                                  foodGroupsService: FoodGroupsAdminService,
                                                  foodCompositionService: FoodCompositionService) extends NutrientMappingService {

  private val logger = LoggerFactory.getLogger(classOf[DefaultNutrientMappingServiceImpl])

  def mapSurveySubmission(submission: SurveySubmission, locale: String): Either[AnyError, NutrientMappedSubmission] = {

    // I have spent an hour writing an "optimal" solution for this, but then realised that food data inheritance is tricky
    // and it is better to solve performance issues using caching :(

    // Lesson learned: trust your past self.

    import uk.ac.ncl.openlab.intake24.errors.ErrorUtils.sequence

    val foodCodes = submission.meals.flatMap(_.foods).map(_.code).distinct

    for (foodData <- sequence(foodCodes.map(foodDataService.getFoodData(_, locale))).right;

         foodDataMap <- Right(foodData.map(_._1).foldLeft(Map[String, ResolvedFoodData]()) {
           case (map, data) => map + (data.code -> data)
         }).right;

         foodGroupMap <- {
           val groupCodes = foodData.map(_._1.groupCode).distinct
           val foodGroupRequests = groupCodes.map {
             groupCode =>
               foodGroupsService.getFoodGroup(groupCode, locale).right.map(g => (groupCode -> g))
           }
           sequence(foodGroupRequests).right.map(_.toMap).right
         };

         compositionDataMap <- {
           val foodCompositionRecords = foodData.flatMap(_._1.nutrientTableCodes).distinct
           val foodCompositionRequests = foodCompositionRecords.map {
             case (tableId, recordId) =>
               foodCompositionService.getFoodCompositionRecord(tableId, recordId).right.map(c => ((tableId, recordId) -> c))
           }

           sequence(foodCompositionRequests).right.map(_.toMap).right
         }
    ) yield {

      val mappedMeals = submission.meals.map {
        meal =>
          val mappedFoods = meal.foods.map {
            food =>

              val psw = food.portionSize.asPortionSizeWithWeights

              val mapping = foodDataMap(food.code).nutrientTableCodes

              if (mapping.size > 1) {
                logger.warn(s"Food ${food.code} has more than one nutrient table mapping for locale $locale (${mapping.keySet.mkString(", ")}, one will be chosen arbitrarily.")
                logger.warn(" ^ please fix this as this leads to undefined behaviour.")
              }

              val foodCompositionData = mapping.headOption match {
                case Some(key) => compositionDataMap(key)
                case None => {
                  logger.error(s"Foods ${food.code} is missing nutrient table mapping for locale $locale. Using empty nutrient record to avoid crashing user-facing service.")
                  Map[Long, Double]()
                }
              }

              val foodData = foodDataMap(food.code)

              val foodGroup = foodGroupMap(foodData.groupCode)

              val nutrients = foodCompositionData.map {
                case (k, v) => (k -> v * psw.portionWeight / 100.0)
              }

              NutrientMappedFood(food.code, foodData.englishDescription, foodData.localDescription, food.isReadyMeal, food.searchTerm, food.brand, psw, food.customData,
                mapping.headOption.map(_._1), mapping.headOption.map(_._2), foodData.reasonableAmount.toDouble >= psw.portionWeight, foodData.groupCode, foodGroup.main.englishDescription, foodGroup.local.localDescription,
                nutrients)

          }

          NutrientMappedMeal(meal.name, meal.time, meal.customData, mappedFoods, meal.missingFoods)

      }

      NutrientMappedSubmission(submission.startTime, submission.endTime, submission.uxSessionId, mappedMeals, submission.customData)
    }
  }
}
