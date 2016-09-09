package uk.ac.ncl.openlab.intake24.foodsql.modular

import scala.Right

import com.google.inject.Inject
import com.google.inject.name.Named

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.CategoryContents
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodBrowsingAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.foodsql.admin.HeaderRows

trait FoodBrowsingAdminQueries
    extends SqlDataService
    with SqlResourceLoader
    with FirstRowValidation
    with HeaderRows {

  private val logger = LoggerFactory.getLogger(classOf[FoodBrowsingAdminQueries])

  lazy private val categoryFoodContentsQuery = sqlFromResource("admin/category_contents_foods.sql")

  protected def categoryFoodContentsQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[FoodHeader]] = {
    val result = SQL(categoryFoodContentsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(code, result, Macro.namedParser[FoodHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.map(_.asFoodHeader))
  }

  lazy private val categorySubcategoryContentsQuery = sqlFromResource("admin/category_contents_subcategories.sql")

  protected def categorySubcategoryContentsQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
    val result = SQL(categorySubcategoryContentsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(code, result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  protected def getCategoryContentsQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, CategoryContents] =
    for (
      foods <- categoryFoodContentsQuery(code, locale).right;
      subcategories <- categorySubcategoryContentsQuery(code, locale).right
    ) yield CategoryContents(foods, subcategories)

  protected def getFoodParentCategoriesHeadersQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
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

    parseWithLocaleAndFoodValidation(code, result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  protected def getCategoryParentCategoriesHeadersQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
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

    parseWithLocaleAndCategoryValidation(code, result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.map(_.asCategoryHeader))
  }
}