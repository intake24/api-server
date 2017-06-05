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
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodBrowsingService
import uk.ac.ncl.openlab.intake24.services.foodindex.{FoodIndex, MatchedFood, Splitter}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.FoodPopularityService
import uk.ac.ncl.openlab.intake24.{UserCategoryHeader, UserFoodHeader}
import io.circe.generic.auto._
import parsers.JsonUtils

import scala.concurrent.Future

case class SplitSuggestion(parts: Seq[String])

case class LookupResult(foods: Seq[UserFoodHeader], categories: Seq[UserCategoryHeader])

class FoodLookupController @Inject()(foodIndexes: Map[String, FoodIndex], foodDescriptionSplitters: Map[String, Splitter],
                                     foodBrowsingService: FoodBrowsingService, foodPopularityService: FoodPopularityService,
                                     rab: Intake24RestrictedActionBuilder) extends Controller with DatabaseErrorHandler with JsonUtils {

  import uk.ac.ncl.openlab.intake24.errors.ErrorUtils._

  def getSplitSuggestion(locale: String, description: String) = rab.restrictToAuthenticated(BodyParsers.parse.empty) {
    _ =>
      Future {
        foodDescriptionSplitters.get(locale) match {
          case Some(splitter) => Ok(toJsonString(SplitSuggestion(splitter.split(description))))
          case None => NotFound(toJsonString(ErrorDescription("InvalidLocale", s"Splitter service not available for locale $locale")))
        }
      }
  }

  private def lookupImpl(locale: String, description: String, maxResults: Int): Either[LookupError, LookupResult] = {
    foodIndexes.get(locale) match {
      case Some(index) => {
        val lookupResult = index.lookup(description, Math.max(0, Math.min(maxResults, 100)))

        foodPopularityService.getPopularityCount(lookupResult.foods.map(_.food.code)).right.map {
          popularityCounters =>

            def adjustedPopularity(f: MatchedFood) = (popularityCounters(f.food.code) + 1.0) / (f.matchCost + 1.0)

            val sortedFoods = lookupResult.foods.sortWith {
              (f1, f2) => adjustedPopularity(f1) > adjustedPopularity(f2)
            }

            val sortedFoodHeaders = sortedFoods.map(_.food)

            val sortedCategoryHeaders = lookupResult.categories.sortBy(_.matchCost).map(_.category)

            LookupResult(sortedFoodHeaders, sortedCategoryHeaders)
        }
      }
      case None => Left(RecordNotFound(new RuntimeException(s"Food index not available for locale $locale")))
    }
  }

  def lookup(locale: String, description: String, maxResults: Int) = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(lookupImpl(locale, description, maxResults))
      }
  }

  //FIXME: bad performance due to individual queries for every food and category
  def lookupInCategory(locale: String, description: String, categoryCode: String, maxResult: Int) = rab.restrictToAuthenticated {
    _ =>
      Future {
        val result = for (
          lookupResult <- lookupImpl(locale, description, maxResult).right;
          foodSuperCategories <- sequence(lookupResult.foods.map(f => foodBrowsingService.getFoodAllCategories(f.code).right.map(sc => (f.code -> sc)))).right.map(_.toMap).right;
          categorySuperCategories <- sequence(lookupResult.categories.map(c => foodBrowsingService.getCategoryAllCategories(c.code).right.map(sc => (c.code -> sc)))).right.map(_.toMap).right
        ) yield LookupResult(lookupResult.foods.filter(f => foodSuperCategories(f.code).contains(categoryCode)),
          lookupResult.categories.filter(c => categorySuperCategories(c.code).contains(categoryCode)))

        translateDatabaseResult(result)
      }
  }
}
