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
import io.circe.generic.auto._
import parsers.JsonUtils
import play.api.mvc.{BaseController, BodyParsers, ControllerComponents, PlayBodyParsers}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodBrowsingService
import uk.ac.ncl.openlab.intake24.services.foodindex.{FoodIndex, IndexLookupResult, MatchedFood, Splitter}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsService, PairwiseAssociationsServiceConfiguration}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.FoodPopularityService
import uk.ac.ncl.openlab.intake24.{UserCategoryHeader, UserFoodHeader}

import scala.concurrent.{ExecutionContext, Future}

case class SplitSuggestion(parts: Seq[String])

case class LookupResult(foods: Seq[UserFoodHeader], categories: Seq[UserCategoryHeader])

class FoodLookupController @Inject()(foodIndexes: Map[String, FoodIndex], foodDescriptionSplitters: Map[String, Splitter],
                                     foodBrowsingService: FoodBrowsingService, foodPopularityService: FoodPopularityService,
                                     pairwiseAssociationsService: PairwiseAssociationsService,
                                     pairwiseAssociationsConfig: PairwiseAssociationsServiceConfiguration,
                                     rab: Intake24RestrictedActionBuilder,
                                     playBodyParsers: PlayBodyParsers,
                                     val controllerComponents: ControllerComponents,
                                     implicit val executionContext: ExecutionContext) extends BaseController with DatabaseErrorHandler with JsonUtils {

  import uk.ac.ncl.openlab.intake24.errors.ErrorUtils._

  def getSplitSuggestion(locale: String, description: String) = rab.restrictToAuthenticated(playBodyParsers.empty) {
    _ =>
      Future {
        foodDescriptionSplitters.get(locale) match {
          case Some(splitter) => Ok(toJsonString(SplitSuggestion(splitter.split(description))))
          case None => NotFound(toJsonString(ErrorDescription("InvalidLocale", s"Splitter service not available for locale $locale")))
        }
      }
  }

  private def lookupImpl(locale: String, description: String, selectedFoods: Seq[String], maxResults: Int): Either[LookupError, LookupResult] = {
    foodIndexes.get(locale) match {
      case Some(index) => {
        val lookupResult = index.lookup(description, Math.max(0, Math.min(maxResults, 100)))

        val sortedFoodHeaders = getSortedFoods(locale, lookupResult).map(_.food)
        val sortedCategoryHeaders = lookupResult.categories.sortBy(_.matchCost).map(_.category)

        Right(LookupResult(sortedFoodHeaders, sortedCategoryHeaders))
      }
      case None => Left(RecordNotFound(new RuntimeException(s"Food index not available for locale $locale")))
    }
  }

  private def getLookupSortMap(locale: String, selectedFoods: Seq[String]): Map[String, Double] = {
    if (selectedFoods.size < pairwiseAssociationsConfig.minInputSearchSize) {
      pairwiseAssociationsService.getOccurrences(locale).map(i => i._1 -> i._2.toDouble)
    } else {
      pairwiseAssociationsService.recommend(locale, selectedFoods).groupBy(_._1).map(i => i._1 -> i._2.head._2)
    }
  }

  private def getSortedFoods(locale: String, lookupResult: IndexLookupResult): Seq[MatchedFood] = {
    val foundFoodCodes = lookupResult.foods.map(_.food.code)
    val srtMap = getLookupSortMap(locale, Nil).filter(i => foundFoodCodes.contains(i._1))
    val getScore = (code: String) => srtMap.getOrElse(code, 0d)

    lookupResult.foods.sortWith {
      (f1, f2) =>
        val pop1 = getScore(f1.food.code)
        val pop2 = getScore(f2.food.code)

        if (pop1 == pop2)
          f1.matchCost < f2.matchCost
        else
          pop1 > pop2
    }
  }

  def lookup(locale: String, description: String, selectedFoods: Seq[String], maxResults: Int) = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(lookupImpl(locale, description, selectedFoods, maxResults))
      }
  }

  //FIXME: bad performance due to individual queries for every food and category
  def lookupInCategory(locale: String, description: String, categoryCode: String, selectedFoods: Seq[String], maxResult: Int) = rab.restrictToAuthenticated {
    _ =>
      Future {
        val result = for (
          lookupResult <- lookupImpl(locale, description, selectedFoods, maxResult).right;
          foodSuperCategories <- sequence(lookupResult.foods.map(f => foodBrowsingService.getFoodAllCategories(f.code).right.map(sc => (f.code -> sc)))).right.map(_.toMap).right;
          categorySuperCategories <- sequence(lookupResult.categories.map(c => foodBrowsingService.getCategoryAllCategories(c.code).right.map(sc => (c.code -> sc)))).right.map(_.toMap).right
        ) yield LookupResult(lookupResult.foods.filter(f => foodSuperCategories(f.code).contains(categoryCode)),
          lookupResult.categories.filter(c => categorySuperCategories(c.code).contains(categoryCode)))

        translateDatabaseResult(result)
      }
  }
}
