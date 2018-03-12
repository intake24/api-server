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
import play.api.mvc.{BaseController, ControllerComponents, PlayBodyParsers}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.data.{ErrorDescription, LookupResult}
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.RecipesAttributeCache
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodBrowsingService
import uk.ac.ncl.openlab.intake24.services.foodindex.{FoodIndex, IndexLookupResult, MatchedFood, Splitter}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsService, PairwiseAssociationsServiceSortTypes}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.FoodPopularityService

import scala.concurrent.{ExecutionContext, Future}

case class SplitSuggestion(parts: Seq[String])

class FoodLookupController @Inject()(foodIndexes: Map[String, FoodIndex],
                                     recipesAttributeIndex: RecipesAttributeCache,
                                     foodDescriptionSplitters: Map[String, Splitter],
                                     foodBrowsingService: FoodBrowsingService, foodPopularityService: FoodPopularityService,
                                     pairwiseAssociationsService: PairwiseAssociationsService,
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

  private def lookupImpl(locale: String, description: String, selectedFoods: Seq[String], maxResults: Int, algorithmId: String, resultFilter: IndexLookupResult => IndexLookupResult): Either[LookupError, LookupResult] = {
    foodIndexes.get(locale) match {
      case Some(index) => {
        val lookupResult = resultFilter(index.lookup(description, Math.max(0, Math.min(maxResults, 50)), 15))

        val sortedFoodHeaders = getSortedFoods(locale, lookupResult, selectedFoods, algorithmId).map(_.food)
        val sortedCategoryHeaders = lookupResult.categories.sortBy(_.matchCost).map(_.category)

        Right(LookupResult(sortedFoodHeaders, sortedCategoryHeaders))
      }
      case None => Left(RecordNotFound(new RuntimeException(s"Food index not available for locale $locale")))
    }
  }

  private def getLookupSortMap(locale: String, selectedFoods: Seq[String], algorithmId: String): Map[String, Double] =
    pairwiseAssociationsService.recommend(locale, selectedFoods, algorithmId).groupBy(_._1).map(i => i._1 -> i._2.head._2)

  private def getSortedFoods(locale: String, lookupResult: IndexLookupResult, selectedFoods: Seq[String], algorithmId: String): Seq[MatchedFood] = {
    val foundFoodCodes = lookupResult.foods.map(_.food.code)
    val gr = getLookupSortMap(locale, selectedFoods, algorithmId)
    val srtMap = gr.filter(i => foundFoodCodes.contains(i._1))
    val getScore = (code: String) => srtMap.getOrElse(code, 0d)

    lookupResult.foods.sortBy(f => (-getScore(f.food.code), f.matchCost))
  }

  def lookup(locale: String, description: String, selectedFoods: Seq[String], maxResults: Int, algorithm: String = PairwiseAssociationsServiceSortTypes.paRules) = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(lookupImpl(locale, description, selectedFoods, maxResults, algorithm, recipesAttributeIndex.filterForRegularFoods))
      }
  }

  def lookupForRecipes(locale: String, description: String, selectedFoods: Seq[String], maxResults: Int, algorithm: String = PairwiseAssociationsServiceSortTypes.paRules) = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(lookupImpl(locale, description, selectedFoods, maxResults, algorithm, recipesAttributeIndex.filterForRecipes))
      }
  }

  //FIXME: bad performance due to individual queries for every food and category
  def lookupInCategory(locale: String, description: String, categoryCode: String, selectedFoods: Seq[String], maxResult: Int, algorithm: String = PairwiseAssociationsServiceSortTypes.paRules) = rab.restrictToAuthenticated {
    _ =>
      Future {
        val result = for (
          lookupResult <- lookupImpl(locale, description, selectedFoods, maxResult, algorithm, recipesAttributeIndex.filterForRegularFoods).right;
          foodSuperCategories <- sequence(lookupResult.foods.map(f => foodBrowsingService.getFoodAllCategories(f.code).right.map(sc => (f.code -> sc)))).right.map(_.toMap).right;
          categorySuperCategories <- sequence(lookupResult.categories.map(c => foodBrowsingService.getCategoryAllCategories(c.code).right.map(sc => (c.code -> sc)))).right.map(_.toMap).right
        ) yield LookupResult(lookupResult.foods.filter(f => foodSuperCategories(f.code).contains(categoryCode)),
          lookupResult.categories.filter(c => categorySuperCategories(c.code).contains(categoryCode)))

        translateDatabaseResult(result)
      }
  }
}
