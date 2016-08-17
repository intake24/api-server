package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.FoodHeader
import anorm._
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.CategoryContents

import anorm.NamedParameter.symbol

import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodBrowsingAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.foodsql.shared.SuperCategoriesImpl
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader

trait FoodBrowsingAdminImpl extends FoodBrowsingAdminService
    with SqlDataService
    with SqlResourceLoader
    with FirstRowValidation
    with HeaderRows
    with SuperCategoriesImpl {

  lazy private val uncategorisedFoodsQuery = sqlFromResource("admin/uncategorised_foods.sql")

  def uncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]] = tryWithConnection {
    implicit conn =>
      val result = SQL(uncategorisedFoodsQuery).on('locale_id -> locale).executeQuery()
      parseWithLocaleValidation(result, Macro.namedParser[FoodHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asFoodHeader))
  }

  lazy private val rootCategoriesQuery = sqlFromResource("admin/root_categories.sql")

  def rootCategories(locale: String): Either[LocaleError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      val result = SQL(rootCategoriesQuery).on('locale_id -> locale).executeQuery()

      parseWithLocaleValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }
  
  lazy private val categoryFoodContentsQuery = sqlFromResource("admin/category_contents_foods.sql")

  private def categoryFoodContentsImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[FoodHeader]] = {
    val result = SQL(categoryFoodContentsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(result, Macro.namedParser[FoodHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asFoodHeader))
  }
  
  lazy private val categorySubcategoryContentsQuery = sqlFromResource("admin/category_contents_subcategories.sql")

  private def categorySubcategoryContentsImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
    val result = SQL(categorySubcategoryContentsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  def categoryContents(code: String, locale: String): Either[LocalLookupError, CategoryContents] = tryWithConnection {
    implicit conn =>
      categoryFoodContentsImpl(code, locale).right.flatMap {
        foods =>
          categorySubcategoryContentsImpl(code, locale).right.flatMap {
            subcategories =>
              Right(CategoryContents(foods, subcategories))
          }
      }
  }

  def foodParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      val result =
        SQL("""|WITH v AS(
               |  SELECT (SELECT code FROM foods WHERE code={food_code}) AS food_code,
               |         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
               |)
               |SELECT v.food_code, v.locale_id, foods_categories.category_code as code, description, is_hidden, local_description
               |FROM v LEFT JOIN foods_categories ON foods_categories.food_code = v.food_code
	             |  LEFT JOIN categories ON categories.code = foods_categories.category_code
               |  LEFT JOIN categories_local ON categories_local.category_code = foods_categories.category_code AND categories_local.locale_id = v.locale_id
               |ORDER BY local_description""".stripMargin)
          .on('food_code -> code, 'locale_id -> locale)
          .executeQuery()

      parseWithLocaleAndFoodValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  def foodAllCategoriesCodes(code: String): Either[LookupError, Seq[String]] = tryWithConnection {
    implicit conn => foodAllCategoriesImpl(code)
  }

  def foodAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn => foodAllCategoriesImpl(code, locale)
  }

  def categoryParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      val query =
        """|WITH v AS(
           |  SELECT (SELECT code FROM categories WHERE code={category_code}) AS category_code,
           |         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
           |)
           |SELECT v.category_code, v.locale_id, categories_categories.category_code as code, description, is_hidden, local_description
           |  FROM v LEFT JOIN categories_categories ON categories_categories.subcategory_code = v.category_code
	         |         LEFT JOIN categories ON categories.code = categories_categories.category_code
           |         LEFT JOIN categories_local ON categories_local.category_code = categories_categories.category_code AND categories_local.locale_id = v.locale_id
           |ORDER BY local_description""".stripMargin

      val result = SQL(query).on('category_code -> code, 'locale_id -> locale).executeQuery()

      parseWithLocaleAndCategoryValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  def categoryAllCategoriesCodes(code: String): Either[LookupError, Seq[String]] = tryWithConnection {
    implicit conn => categoryAllCategoriesImpl(code)
  }

  def categoryAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn => categoryAllCategoriesImpl(code, locale)
  }
}