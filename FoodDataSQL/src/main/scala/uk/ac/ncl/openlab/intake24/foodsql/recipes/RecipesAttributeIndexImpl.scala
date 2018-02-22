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

package uk.ac.ncl.openlab.intake24.foodsql.recipes

import javax.sql.DataSource

import anorm.{Macro, SQL, sqlToSimple}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.foodindex.IndexLookupResult
import uk.ac.ncl.openlab.intake24.services.{RecipesAttributeIndex, UseInRecipes}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}


@Singleton
class RecipesAttributeIndexImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends SqlDataService
  with SqlResourceLoader with RecipesAttributeIndex {

  private val logger = LoggerFactory.getLogger(classOf[RecipesAttributeIndexImpl])

  private case class UseInRecipesRow(food_code: String, use_in_recipes: Int)

  def throwOnError[T](result: Either[AnyError, T]): T = result match {
    case Left(e) => throw e.exception
    case Right(r) => r
  }

  val useInRecipesAttributes = throwOnError(tryWithConnection {
    implicit conn =>
      val useInRecipesQuery = sqlFromResource("foodindex/inherited_attributes_recipes.sql")

      Right(SQL(useInRecipesQuery)
        .executeQuery()
        .as(Macro.namedParser[UseInRecipesRow].*)
        .foldLeft(Map[String, UseInRecipes]()) {
          case (map, row) =>

            val useInRecipes = row.use_in_recipes match {
              case 0 => UseInRecipes.Anywhere
              case 1 => UseInRecipes.RegularFoodsOnly
              case 2 => UseInRecipes.RecipesOnly
            }

            map + (row.food_code -> useInRecipes)
        })
  })

  private def filterResults(indexLookupResult: IndexLookupResult, useForRecipes: Boolean) =
    IndexLookupResult(
      indexLookupResult.foods.filter {
        food =>

          val attr = useInRecipesAttributes.getOrElse(food.food.code, UseInRecipes.Anywhere)

          attr match {
            case UseInRecipes.Anywhere => true
            case UseInRecipes.RecipesOnly => useForRecipes
            case UseInRecipes.RegularFoodsOnly => !useForRecipes
          }
      },
      indexLookupResult.categories)

  def filterForRegularFoods(indexLookupResult: IndexLookupResult) = filterResults(indexLookupResult, false)

  def filterForRecipes(indexLookupResult: IndexLookupResult) = filterResults(indexLookupResult, true)
}
