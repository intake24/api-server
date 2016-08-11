package uk.ac.ncl.openlab.intake24.foodsql.user

import scala.Right

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.UserCategoryContents
import uk.ac.ncl.openlab.intake24.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.UserFoodHeader
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalCategoryCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodBrowsingService

trait FoodBrowsingUserImpl extends FoodBrowsingService with SqlDataService with SqlResourceLoader with FirstRowValidation {

  private lazy val rootCategoriesQuery = sqlFromResource("user/root_categories.sql")

  private case class UserCategoryHeaderRow(code: String, description: String, local_description: Option[String]) {
    def mkUserHeader = UserCategoryHeader(code, local_description.getOrElse(description))
  }

  def rootCategories(locale: String): Either[LocaleError, Seq[UserCategoryHeader]] = tryWithConnection {
    implicit conn =>
      val result = SQL(rootCategoriesQuery).on('locale_id -> locale).executeQuery()

      parseWithLocaleValidation(result, Macro.namedParser[UserCategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map {
        _.map {
          _.mkUserHeader
        }
      }
  }

  private lazy val categoryFoodContentsQuery = sqlFromResource("user/category_contents_foods.sql")

  private case class UserFoodHeaderRow(code: String, description: String, local_description: Option[String]) {
    def mkUserHeader = UserFoodHeader(code, local_description.getOrElse(description))
  }

  private def categoryFoodContentsImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalCategoryCodeError, Seq[UserFoodHeader]] = {

    val result = SQL(categoryFoodContentsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(result, Macro.namedParser[UserFoodHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map {
      _.map {
        _.mkUserHeader
      }
    }
  }

  private lazy val categorySubcategoryContentsQuery = sqlFromResource("user/category_contents_subcategories.sql")

  private def categorySubcategoryContentsImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalCategoryCodeError, Seq[UserCategoryHeader]] = {
    val result = SQL(categorySubcategoryContentsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(result, Macro.namedParser[UserCategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map {
      _.map {
        _.mkUserHeader
      }
    }
  }

  def categoryContents(code: String, locale: String): Either[LocalCategoryCodeError, UserCategoryContents] = tryWithConnection {
    implicit conn =>
      for (
        foods <- categoryFoodContentsImpl(code, locale).right;
        subcategories <- categorySubcategoryContentsImpl(code, locale).right
      ) yield {
        UserCategoryContents(foods, subcategories)
      }
  }
}