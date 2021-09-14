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
import org.slf4j.LoggerFactory
import parsers.JsonUtils
import play.api.mvc.{BaseController, ControllerComponents, PlayBodyParsers}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.data.{ErrorDescription, LookupResult, UserFoodHeader}
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

  private val logger = LoggerFactory.getLogger(getClass)

  def getSplitSuggestion(locale: String, description: String) = rab.restrictToAuthenticated(playBodyParsers.empty) {
    _ =>
      Future {
        foodDescriptionSplitters.get(locale) match {
          case Some(splitter) => Ok(toJsonString(SplitSuggestion(splitter.split(description))))
          case None => NotFound(toJsonString(ErrorDescription("InvalidLocale", s"Splitter service not available for locale $locale")))
        }
      }
  }

  private def lookupImpl(locale: String, description: String, selectedFoods: Seq[String], maxResults: Int, algorithmId: String, matchScoreWeight: Double, resultFilter: IndexLookupResult => IndexLookupResult): Either[LookupError, LookupResult] = {
    foodIndexes.get(locale) match {
      case Some(index) => {
        val lookupResult = resultFilter(index.lookup(description, Math.max(0, Math.min(maxResults, 50)), 15))

        logger.debug(lookupResult.toString)

        val sortedFoodHeaders = getSortedFoods(locale, lookupResult, selectedFoods, algorithmId, matchScoreWeight).map(_.food)
        val sortedCategoryHeaders = lookupResult.categories.sortBy(_.matchCost).map(_.category)

        Right(LookupResult(sortedFoodHeaders, sortedCategoryHeaders))
      }
      case None => Left(RecordNotFound(new RuntimeException(s"Food index not available for locale $locale")))
    }
  }

  private def normaliseSortMap(m: Map[String, Double]): Map[String, Double] = {
    if (m.isEmpty)
      Map()
    else {
      val min = m.values.min
      val max = m.values.max

      val range = max - min

      if (range.abs < 1E-8) {
        // Map same values to 1.0
        m.mapValues(v => v - min + 1.0).map(identity)
      } else {
        m.mapValues(v => (v - min) / range).map(identity)
      }
    }
  }

  private def normaliseMatchCost(foods: Seq[MatchedFood]): Seq[MatchedFood] = {
    if (foods.isEmpty)
      Seq()
    else {

      val costs = foods.map(_.matchCost)

      val min = costs.min
      val max = costs.max

      val range = max - min

      if (range.abs < 1E-8) {
        foods.map(f => MatchedFood(f.food, 1.0 - f.matchCost - min))
      } else {
        foods.map(f => MatchedFood(f.food, 1.0 - (f.matchCost - min) / range))
      }
    }
  }

  private def getLookupSortMap(locale: String, selectedFoods: Seq[String], algorithmId: String): Map[String, Double] = algorithmId match {
    case "paRules" | "popularity" => pairwiseAssociationsService.recommend(locale, selectedFoods, algorithmId).groupBy(_._1).map(i => i._1 -> i._2.head._2)
    case _ => throw new RuntimeException(s"Unexpected sort algorithm id: $algorithmId");
  }

  private def getSortedFoods(locale: String, lookupResult: IndexLookupResult, selectedFoods: Seq[String], algorithmId: String, matchScoreWeight: Double): Seq[MatchedFood] = {
    val foundFoodCodes = lookupResult.foods.map(_.food.code)
    val gr = getLookupSortMap(locale, selectedFoods, algorithmId)
    val srtMap = normaliseSortMap(gr.filter(i => foundFoodCodes.contains(i._1)))
    val normalisedMatchedFoods = normaliseMatchCost(lookupResult.foods)

    val getScore = (code: String) => srtMap.getOrElse(code, 0d)

    normalisedMatchedFoods.sortBy(f => {
      -(getScore(f.food.code) * (1.0 - matchScoreWeight) + f.matchCost * matchScoreWeight)
    })
  }

  def lookup(locale: String, description: String, selectedFoods: Seq[String], maxResults: Int, algorithm: String, matchScoreWeight: Int) = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(lookupImpl(locale, description, selectedFoods, maxResults, algorithm, matchScoreWeight / 100.0, recipesAttributeIndex.filterForRegularFoods))
      }
  }

  def lookupForRecipes(locale: String, description: String, selectedFoods: Seq[String], maxResults: Int, algorithm: String, matchScoreWeight: Int) = rab.restrictToAuthenticated {
    _ =>
      Future {
        translateDatabaseResult(lookupImpl(locale, description, selectedFoods, maxResults, algorithm, matchScoreWeight / 100.0, recipesAttributeIndex.filterForRecipes))
      }
  }

  //FIXME: bad performance due to individual queries for every food and category
  def lookupInCategory(locale: String, description: String, categoryCode: String, selectedFoods: Seq[String], maxResult: Int, algorithm: String, matchScoreWeight: Int) = rab.restrictToAuthenticated {
    _ =>
      Future {
        val result = for (
          lookupResult <- lookupImpl(locale, description, selectedFoods, maxResult, algorithm, matchScoreWeight / 100.0, recipesAttributeIndex.filterForRegularFoods).right;
          foodSuperCategories <- sequence(lookupResult.foods.map(f => foodBrowsingService.getFoodAllCategories(f.code).right.map(sc => (f.code -> sc)))).right.map(_.toMap).right;
          categorySuperCategories <- sequence(lookupResult.categories.map(c => foodBrowsingService.getCategoryAllCategories(c.code).right.map(sc => (c.code -> sc)))).right.map(_.toMap).right
        ) yield LookupResult(lookupResult.foods.filter(f => foodSuperCategories(f.code).contains(categoryCode)),
          lookupResult.categories.filter(c => categorySuperCategories(c.code).contains(categoryCode)))

        translateDatabaseResult(result)
      }
  }
}
