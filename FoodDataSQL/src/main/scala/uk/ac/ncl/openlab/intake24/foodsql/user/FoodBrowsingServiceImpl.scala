package uk.ac.ncl.openlab.intake24.foodsql.user

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, SqlParser, sqlToSimple}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.api.data.admin.{CategoryHeader, FoodHeader}
import uk.ac.ncl.openlab.intake24.api.data.{UserCategoryContents, UserCategoryHeader, UserFoodHeader}
import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, LocaleError, LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.foodsql.shared.SuperCategoriesQueries
import uk.ac.ncl.openlab.intake24.foodsql.{FirstRowValidation, FirstRowValidationClause}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{CategoryCategoryRelation, FoodBrowsingService, FoodCategoryRelation}
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

  private def validateCategoryAndLocale(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Unit] = {
    val validation = SQL("select (select 1 from categories where code={category_code}) as v1, (select 1 from locales where id={locale_id}) as v2")
      .on('category_code -> code, 'locale_id -> locale)
      .executeQuery()
      .as((SqlParser.int(1).? ~ SqlParser.int(2).?).single)

    if (validation._1.isEmpty)
      Left(RecordNotFound(new RuntimeException(s"Category $code does not exist")))
    else if (validation._2.isEmpty)
      Left(RecordNotFound(new RuntimeException(s"Locale $locale does not exist")))
    else
      Right(())
  }

  private def categoryFoodContentsImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[UserFoodHeader]] = {
    validateCategoryAndLocale(code, locale).flatMap {
      _ =>
        Right(SQL(categoryFoodContentsQuery).on('category_code -> code, 'locale_id -> locale)
          .executeQuery()
          .as(Macro.namedParser[UserFoodHeaderRow].*)
          .map(_.mkUserHeader))
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

  override def listFodCategoryRelationships(): Either[LookupError, Seq[FoodCategoryRelation]] = tryWithConnection {
    implicit conn =>
      val q =
        """
          |SELECT food_code AS foodCode, category_code AS categoryCode FROM foods_categories
          |JOIN categories ON categories.code=foods_categories.category_code
          |WHERE categories.is_hidden=FALSE
        """.stripMargin
      val rows = SQL(q).as(Macro.namedParser[FoodCategoryRelation].*)
      Right(rows)
  }

  override def listCategoryCategoryRelationships(): Either[LookupError, Seq[CategoryCategoryRelation]] = tryWithConnection {
    implicit conn =>
      val q =
        """
          |SELECT category_code AS categoryCode, subcategory_code AS subCategoryCode FROM categories_categories
          |JOIN categories ON categories.code=categories_categories.category_code
          |WHERE categories.is_hidden=FALSE
        """.stripMargin
      val rows = SQL(q).as(Macro.namedParser[CategoryCategoryRelation].*)
      Right(rows)
  }

  override def listAllFoods(localeId: String): Either[LookupError, Seq[UserFoodHeader]] = tryWithConnection {
    implicit conn =>
      val q =
        """
          |SELECT
          |  f.code,
          |  f2.local_description AS localDescription
          |FROM foods AS f
          |  JOIN foods_local f2 on f.code = f2.food_code
          |WHERE f2.locale_id='en_GB' AND NOT f2.do_not_use AND f2.local_description IS NOT NULL
        """.stripMargin
      val rows = SQL(q).on('locale_id -> localeId).as(Macro.namedParser[UserFoodHeader].*)
      Right(rows)
  }

}
