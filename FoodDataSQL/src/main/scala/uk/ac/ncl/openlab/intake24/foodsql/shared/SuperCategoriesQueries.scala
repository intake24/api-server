package uk.ac.ncl.openlab.intake24.foodsql.shared

import anorm.SqlParser
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import anorm.Macro
import anorm.SQL
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.foodsql.admin.HeaderRows
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader

trait SuperCategoriesQueries extends FirstRowValidation with HeaderRows with SqlResourceLoader {

  private lazy val foodAllCategoriesCodesQuery = sqlFromResource("shared/food_all_categories_codes_frv.sql")

  protected def getFoodAllCategoriesCodesQuery(code: String)(implicit conn: java.sql.Connection): Either[LookupError, Set[String]] = {
    val result = SQL(foodAllCategoriesCodesQuery).on('food_code -> code).executeQuery()

    parseWithFoodValidation(code, result, SqlParser.str("code").+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.toSet)
  }

  private lazy val foodAllCategoriesHeadersQuery = sqlFromResource("shared/food_all_categories_headers_frv.sql")

  protected def getFoodAllCategoriesHeadersQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
    val result = SQL(foodAllCategoriesHeadersQuery).on('food_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndFoodValidation(code, result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  private lazy val categoryAllCategoriesCodesQuery = sqlFromResource("shared/categories_all_categories_codes_frv.sql")

  def getCategoryAllCategoriesCodesQuery(code: String)(implicit conn: java.sql.Connection): Either[LookupError, Set[String]] = {
    val result = SQL(categoryAllCategoriesCodesQuery).on('category_code -> code).executeQuery()

    parseWithCategoryValidation(code, result, SqlParser.str("code").+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.toSet)
  }

  private lazy val categoryAllCategoriesHeadersQuery = sqlFromResource("shared/categories_all_categories_headers_frv.sql")

  def getCategoryAllCategoriesHeadersQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
    val result = SQL(categoryAllCategoriesHeadersQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(code, result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }
}
