package uk.ac.ncl.openlab.intake24.foodsql.user

import javax.sql.DataSource

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, SqlParser, sqlToSimple}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import uk.ac.ncl.openlab.intake24.api.data.admin.CategoryHeader
import uk.ac.ncl.openlab.intake24.api.data.{UserCategoryContents, UserCategoryHeader, UserFoodHeader}
import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, LocaleError, LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.foodsql.shared.SuperCategoriesQueries
import uk.ac.ncl.openlab.intake24.foodsql.{FirstRowValidation, FirstRowValidationClause}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodBrowsingService
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

@Singleton
class FoodBrowsingServiceImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends FoodBrowsingService with SqlDataService with SqlResourceLoader with FirstRowValidation with SuperCategoriesQueries {

  private lazy val rootCategoriesQuery = sqlFromResource("user/root_categories.sql")
  private lazy val getFoodCategoriesWithLevelsQuery = sqlFromResource("user/get_categories_with_levels_by_food_and_locale.sql")

  private case class FoodCategoryWithLevelRow(food_code: String, code: String, description: String, local_description: String, level: Int, is_hidden: Boolean)

  private case class UserCategoryHeaderRow(code: String, description: String, local_description: Option[String]) {
    def mkUserHeader = UserCategoryHeader(code, local_description.getOrElse(description))
  }

  def getRootCategories(locale: String): Either[LocaleError, Seq[UserCategoryHeader]] = tryWithConnection {
    implicit conn =>
      val result = SQL(rootCategoriesQuery).on('locale_id -> locale).executeQuery()

      parseWithLocaleValidation(result, Macro.namedParser[UserCategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map {
        _.map {
          _.mkUserHeader
        }
      }
  }

  private lazy val categoryFoodContentsQuery = sqlFromResource("user/category_contents_foods.sql")

  private case class UserFoodHeaderRow(code: String, description: String, local_description: Option[String]) {
    def mkUserHeader = UserFoodHeader(code, local_description.getOrElse(description))
  }

  private def categoryFoodContentsImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[UserFoodHeader]] = {

    val result = SQL(categoryFoodContentsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(code, result, Macro.namedParser[UserFoodHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map {
      _.map {
        _.mkUserHeader
      }
    }
  }

  private lazy val categorySubcategoryContentsQuery = sqlFromResource("user/category_contents_subcategories.sql")

  private def categorySubcategoryContentsImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[UserCategoryHeader]] = {
    val result = SQL(categorySubcategoryContentsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(code, result, Macro.namedParser[UserCategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map {
      _.map {
        _.mkUserHeader
      }
    }
  }

  def getCategoryContents(code: String, locale: String): Either[LocalLookupError, UserCategoryContents] = tryWithConnection {
    implicit conn =>
      for (
        foods <- categoryFoodContentsImpl(code, locale).right;
        subcategories <- categorySubcategoryContentsImpl(code, locale).right
      ) yield {
        UserCategoryContents(foods, subcategories)
      }
  }

  def getFoodAllCategories(code: String): Either[LookupError, Set[String]] = tryWithConnection {
    implicit conn => getFoodAllCategoriesCodesQuery(code)
  }

  def getCategoryAllCategories(code: String): Either[LookupError, Set[String]] = tryWithConnection {
    implicit conn => getCategoryAllCategoriesCodesQuery(code)
  }

  override def getFoodCategories(code: String, localeId: String, level: Int): Either[LookupError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val foodOpt = SQL("SELECT code FROM foods WHERE code = {food_code};").on('food_code -> code).as(SqlParser.str("code").singleOpt)
        val localeOpt = SQL("SELECT id FROM locales WHERE id = {locale};").on('locale -> localeId).as(SqlParser.str("id").singleOpt)
        (foodOpt, localeOpt) match {
          case (Some(_), Some(_)) =>
            val rows = SQL(getFoodCategoriesWithLevelsQuery).on('food_code -> code, 'locale_id -> localeId).executeQuery().as(Macro.namedParser[FoodCategoryWithLevelRow].*)
            var lev = level
            var categories: List[FoodCategoryWithLevelRow] = Nil
            while (lev >= 0 && categories.isEmpty) {
              categories = rows.filter(r => r.level == lev)
              lev -= 1
            }
            Right(categories.map(c => CategoryHeader(c.code, c.description, Some(c.local_description), c.is_hidden)))
          case _ => Left(RecordNotFound(new Exception(s"No food found with code: $code")))
        }
      }
  }

}
