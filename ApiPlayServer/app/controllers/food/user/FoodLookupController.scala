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

package controllers.food.user

import javax.inject.Inject

import controllers.DatabaseErrorHandler
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller}
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24.{UserCategoryHeader, UserFoodHeader}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.foodindex.{FoodIndex, MatchedFood, Splitter}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.FoodPopularityService
import upickle.default._

import scala.concurrent.Future

case class SplitSuggestion(parts: Seq[String])

case class LookupResult(foods: Seq[UserFoodHeader], categories: Seq[UserCategoryHeader])

class FoodLookupController @Inject()(foodIndexes: Map[String, FoodIndex], foodDescriptionSplitters: Map[String, Splitter],
                                     foodPopularityService: FoodPopularityService,
                                     deadbolt: DeadboltActionsAdapter) extends Controller with DatabaseErrorHandler {

  def getSplitSuggestion(locale: String, description: String) = deadbolt.restrictToRespondents(BodyParsers.parse.empty) {
    _ =>
      Future {
        foodDescriptionSplitters.get(locale) match {
          case Some(splitter) => Ok(write(SplitSuggestion(splitter.split(description))))
          case None => NotFound(write(ErrorDescription("InvalidLocale", s"Splitter service not available for locale $locale")))
        }
      }
  }

  def lookup(locale: String, description: String, maxResults: Int) = deadbolt.restrictToRespondents(BodyParsers.parse.empty) {
    _ =>
      Future {
        foodIndexes.get(locale) match {
          case Some(index) => {
            val lookupResult = index.lookup(description, Math.min(0, Math.max(maxResults, 100)))

            translateDatabaseResult(foodPopularityService.getPopularityCount(lookupResult.foods.map(_.food.code)).right.map {
              popularityCounters =>

                def adjustedPopularity(f: MatchedFood) = (popularityCounters(f.food.code) + 1.0) / (f.matchCost + 1.0)

                val sortedFoods = lookupResult.foods.sortWith {
                  (f1, f2) => adjustedPopularity(f1) > adjustedPopularity(f2)
                }

                val sortedFoodHeaders = sortedFoods.map(_.food)

                val sortedCategoryHeaders = lookupResult.categories.sortBy(_.matchCost).map(_.category)

                LookupResult(sortedFoodHeaders, sortedCategoryHeaders)
            })
          }
          case None => NotFound(write(ErrorDescription("InvalidLocale", s"Food index not available for locale $locale")))
        }
      }
  }
}
