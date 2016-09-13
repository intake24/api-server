package uk.ac.ncl.openlab.intake24.foodsql.admin

import scala.Right

import org.slf4j.LoggerFactory

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.CategoryContents
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.foodsql.modular.FoodBrowsingAdminQueries
import uk.ac.ncl.openlab.intake24.foodsql.shared.SuperCategoriesQueries
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodBrowsingAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError

@Singleton
class FoodBrowsingAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends FoodBrowsingAdminImpl

trait FoodBrowsingAdminImpl extends FoodBrowsingAdminService
    with FoodBrowsingAdminQueries
    with SuperCategoriesQueries {

  private val logger = LoggerFactory.getLogger(classOf[FoodBrowsingAdminImpl])

  lazy private val uncategorisedFoodsQuery = sqlFromResource("admin/uncategorised_foods.sql")

  def getUncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]] = tryWithConnection {
    implicit conn =>
      val result = SQL(uncategorisedFoodsQuery).on('locale_id -> locale).executeQuery()
      parseWithLocaleValidation(result, Macro.namedParser[FoodHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.map(_.asFoodHeader))
  }

  lazy private val rootCategoriesQuery = sqlFromResource("admin/root_categories.sql")

  def getRootCategories(locale: String): Either[LocaleError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      val result = SQL(rootCategoriesQuery).on('locale_id -> locale).executeQuery()

      parseWithLocaleValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  def getCategoryContents(code: String, locale: String): Either[LocalLookupError, CategoryContents] = tryWithConnection {
    implicit conn =>
      withTransaction {
        getCategoryContentsQuery(code, locale)
      }
  }

  def getFoodParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      getFoodParentCategoriesHeadersQuery(code, locale)
  }

  def getCategoryParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      getCategoryParentCategoriesHeadersQuery(code, locale)
  }

  def getFoodAllCategoriesCodes(code: String): Either[LookupError, Set[String]] = tryWithConnection {
    implicit conn => getFoodAllCategoriesCodesQuery(code)
  }

  def getFoodAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn => getFoodAllCategoriesHeadersQuery(code, locale)
  }

  def getCategoryAllCategoriesCodes(code: String): Either[LookupError, Set[String]] = tryWithConnection {
    implicit conn => getCategoryAllCategoriesCodesQuery(code)
  }

  def getCategoryAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn => getCategoryAllCategoriesHeadersQuery(code, locale)
  }
}