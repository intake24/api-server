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

package uk.ac.ncl.openlab.intake24.foodsql

import java.sql.Connection
import java.util.UUID
import org.slf4j.LoggerFactory
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.SqlParser.str
import anorm.sqlToSimple
import javax.sql.DataSource
import net.scran24.fooddef.AsServedImage
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.CategoryContents
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.DrinkScale
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.Food
import net.scran24.fooddef.FoodData
import net.scran24.fooddef.FoodGroup
import net.scran24.fooddef.FoodHeader
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.GuideImageWeightRecord
import net.scran24.fooddef.InheritableAttributes
import net.scran24.fooddef.PortionSizeMethod
import net.scran24.fooddef.PortionSizeMethodParameter
import net.scran24.fooddef.Prompt
import net.scran24.fooddef.SplitList
import net.scran24.fooddef.VolumeFunction
import net.scran24.fooddef.FoodLocal
import net.scran24.fooddef.Category
import net.scran24.fooddef.CategoryLocal
import anorm.SqlParser
import net.scran24.fooddef.GuideHeader
import net.scran24.fooddef.AsServedHeader
import net.scran24.fooddef.DrinkwareHeader
import net.scran24.fooddef.NutrientTable
import anorm.SqlMappingError
import anorm.AnormException
import uk.ac.ncl.openlab.intake24.services.AdminFoodDataService
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService
import net.scran24.fooddef.UserFoodHeader
import net.scran24.fooddef.UserCategoryHeader
import net.scran24.fooddef.UserCategoryContents

@Singleton
class UserFoodDataServiceSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends UserFoodDataService
    with SqlDataService with FoodDataSqlImpl with PortionSizeDataSqlImpl {

  import UserFoodDataServiceSqlImpl._

  val logger = LoggerFactory.getLogger(classOf[UserFoodDataServiceSqlImpl])

  def rootCategories(locale: String): Seq[UserCategoryHeader] = tryWithConnection {
    implicit conn =>
      SQL(rootCategoriesQuery).on('locale_id -> locale).executeQuery().as(Macro.indexedParser[UserCategoryHeader].*)
  }

  def categoryContents(code: String, locale: String): UserCategoryContents = tryWithConnection {
    implicit conn =>
      val foods = SQL(categoryContentsFoodsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery().as(Macro.indexedParser[UserFoodHeader].*)

      val categories = SQL(categoryContentsSubcategoriesQuery).on('category_code -> code, 'locale_id -> locale).executeQuery().as(Macro.indexedParser[UserCategoryHeader].*)

      UserCategoryContents(foods, categories)
  }

  def associatedFoodPrompts(foodCode: String, locale: String): Seq[Prompt] = tryWithConnection {
    implicit conn =>
      val query =
        """SELECT category_code, text, link_as_main, generic_name FROM associated_food_prompts WHERE food_code = {food_code} AND locale_id = {locale_id} ORDER BY id"""

      SQL(query).on('food_code -> foodCode, 'locale_id -> locale).executeQuery().as(Macro.parser[Prompt]("category_code", "text", "link_as_main", "generic_name").*)
  }

  def brandNames(foodCode: String, locale: String): Seq[String] = tryWithConnection {
    implicit conn =>
      SQL("""SELECT name FROM brands WHERE food_code = {food_code} AND locale_id = {locale_id} ORDER BY id""")
        .on('food_code -> foodCode, 'locale_id -> locale)
        .executeQuery()
        .as(str("name").*)
  }
}

object UserFoodDataServiceSqlImpl {
  val rootCategoriesQuery = io.Source.fromInputStream(getClass.getResourceAsStream("sql/user/root_categories.sql")).mkString
  val categoryContentsFoodsQuery = io.Source.fromInputStream(getClass.getResourceAsStream("sql/user/category_contents_foods.sql")).mkString
  val categoryContentsSubcategoriesQuery = io.Source.fromInputStream(getClass.getResourceAsStream("sql/user/category_contents_subcategories.sql")).mkString
}