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
import uk.ac.ncl.openlab.intake24._
import anorm.SqlParser
import anorm.SqlMappingError
import anorm.AnormException
import uk.ac.ncl.openlab.intake24.services.AdminFoodDataService
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService
import uk.ac.ncl.openlab.intake24.services.CodeError

@Singleton
class UserFoodDataServiceSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends UserFoodDataService
    with SqlDataService with FoodDataSqlImpl with PortionSizeDataSqlImpl {

  import UserFoodDataServiceSqlImpl._

  val logger = LoggerFactory.getLogger(classOf[UserFoodDataServiceSqlImpl])

  def rootCategories(locale: String): Seq[UserCategoryHeader] = tryWithConnection {
    implicit conn =>
      SQL(rootCategoriesQuery).on('locale_id -> locale).executeQuery().as(Macro.indexedParser[UserCategoryHeader].*)
  }

  case class CategoryContentsFoodRow(food_code: String, local_description: String)

  case class CategoryContentsCategoryRow(subcategory_code: String, local_description: String)

  def categoryContents(code: String, locale: String) = tryWithConnection {
    implicit conn =>
      val categoryAllowed = (SQL(categoryRestrictions).on('category_code -> code, 'locale_id -> locale).executeQuery().as(SqlParser.long(1).single) == 1)

      if (!categoryAllowed)
        Left(CodeError.UndefinedCode)
      else {
        val foodRows = SQL(categoryContentsFoodsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery().as(Macro.namedParser[CategoryContentsFoodRow].*)
        val categoryRows = SQL(categoryContentsSubcategoriesQuery).on('category_code -> code, 'locale_id -> locale).executeQuery().as(Macro.namedParser[CategoryContentsCategoryRow].*)
        
        Right(UserCategoryContents(foodRows.map(row => UserFoodHeader(row.food_code, row.local_description)), categoryRows.map(row => UserCategoryHeader(row.subcategory_code, row.local_description))))
      }
  }

  case class AssociatedFoodPromptsRow(category_code: Option[String], text: Option[String], link_as_main: Option[Boolean], generic_name: Option[String], locale_id: Option[String])

  def associatedFoods(foodCode: String, locale: String): Either[CodeError, Seq[AssociatedFood]] = tryWithConnection {
    implicit conn =>
      val query =
        """|SELECT category_code, text, link_as_main, generic_name, locale_id
           |FROM foods
	         |  LEFT JOIN associated_food_prompts 
		       |    ON foods.code = associated_food_prompts.food_code
           |WHERE 
           |  foods.code = {food_code} 
           |  AND (locale_id = {locale_id} OR locale_id IS NULL OR locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id})) 
           |ORDER BY id""".stripMargin

      val rows = SQL(query).on('food_code -> foodCode, 'locale_id -> locale).executeQuery().as(Macro.namedParser[AssociatedFoodPromptsRow].*)

      def mkPrompt(row: AssociatedFoodPromptsRow) = AssociatedFood(row.category_code.get, row.text.get, row.link_as_main.get, row.generic_name.get)

      if (rows.isEmpty)
        Left(CodeError.UndefinedCode)
      else if (rows.head.category_code.isEmpty)
        Right(Seq())
      else {
        val (local, prototype) = rows.partition(_.locale_id.get == locale)

        if (local.nonEmpty)
          Right(local.map(mkPrompt))
        else
          Right(prototype.map(mkPrompt))
      }
  }

  case class BrandNamesRow(name: Option[String], locale_id: Option[String])

  def brandNames(foodCode: String, locale: String): Either[CodeError, Seq[String]] = tryWithConnection {
    implicit conn =>

      val query =
        """|SELECT name, locale_id
	         |FROM foods
	         |  LEFT JOIN brands
		       |    ON foods.code = brands.food_code
           |WHERE foods.code = {food_code} AND (locale_id = {locale_id} OR locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id}) OR locale_id IS NULL) ORDER BY id""".stripMargin

      val rows = SQL(query).on('food_code -> foodCode, 'locale_id -> locale).executeQuery().as(Macro.namedParser[BrandNamesRow].*)

      if (rows.isEmpty)
        Left(CodeError.UndefinedCode)
      else if (rows.head.name.isEmpty)
        Right(Seq())
      else {
        val (local, prototype) = rows.partition(_.locale_id.get == locale)

        if (local.nonEmpty)
          Right(local.map(_.name.get))
        else
          Right(prototype.map(_.name.get))
      }
  }
}

object UserFoodDataServiceSqlImpl {
  val rootCategoriesQuery = io.Source.fromInputStream(getClass.getResourceAsStream("/sql/user/root_categories.sql")).mkString
  /* 
   Build food headers for foods contained in the given category for the
   given locale.

   Returns one of the following:

   1) An empty set of rows if the category code is undefined or the category is
   excluded from the current locale  (the category can be excluded in two ways:
   through the global restriction list and through the local "do not use in this
   locale" flag)

   2) A single row with null food_code field if the category exists but has no
   foods that are allowed in the current locale

   3) One or more rows with non-null food_code field if the category exists, is
   allowed in the current locale, and is not empty

   Note: categories that don't pass the restriction filter are treated as
   non-existing.  
  */
  val categoryContentsFoodsQuery = io.Source.fromInputStream(getClass.getResourceAsStream("/sql/user/category_contents_foods.sql")).mkString

  /* 
   Build subcategory headers for foods contained in the given category for the
   given locale. Works similar to foods query above.
  */
  val categoryContentsSubcategoriesQuery = io.Source.fromInputStream(getClass.getResourceAsStream("/sql/user/category_contents_subcategories.sql")).mkString

  val categoryRestrictions = io.Source.fromInputStream(getClass.getResourceAsStream("/sql/user/category_restrictions.sql")).mkString
}
