package uk.ac.ncl.openlab.intake24.foodsql.admin

import javax.sql.DataSource

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, sqlToSimple}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.data.admin.{CategoryContents, CategoryHeader, FoodHeader}
import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, LocaleError, LookupError}
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.foodsql.modular.FoodBrowsingAdminQueries
import uk.ac.ncl.openlab.intake24.foodsql.shared.SuperCategoriesQueries
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{CategoryDescendantsCodes, FoodBrowsingAdminService}


@Singleton
class FoodBrowsingAdminImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends FoodBrowsingAdminService
  with FoodBrowsingAdminQueries
  with SuperCategoriesQueries {

  private val logger = LoggerFactory.getLogger(classOf[FoodBrowsingAdminImpl])

  lazy private val uncategorisedFoodsQuery = sqlFromResource("admin/uncategorised_foods.sql")

  def getUncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]] = tryWithConnection {
    implicit conn =>
      val result = SQL(uncategorisedFoodsQuery).on('locale_id -> locale).executeQuery()
      parseWithLocaleValidation(result, Macro.namedParser[FoodHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.map(_.asFoodHeader(locale)))
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

  def getAllCategoryDescendantsCodes(code: String): Either[LookupError, CategoryDescendantsCodes] = tryWithConnection {
    implicit conn =>

      def rec(queue: Set[String], acc: CategoryDescendantsCodes): Either[LookupError, CategoryDescendantsCodes] = queue.headOption match {
        case Some(code) =>
          categoryFoodContentsCodesQuery(code).right.flatMap {
            foodCodes =>
              categorySubcategoryContentsCodesQuery(code).right.flatMap {
                subcategoryCodes =>
                  rec(subcategoryCodes ++ queue.tail, CategoryDescendantsCodes(acc.foods ++ foodCodes, acc.subcategories ++ subcategoryCodes))
              }
          }
        case None => Right(acc)
      }

      rec(Set(code), CategoryDescendantsCodes(Set[String](), Set[String]()))
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