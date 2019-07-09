package uk.ac.ncl.openlab.intake24.foodsql.modular

import anorm.Macro.ColumnNaming
import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, SqlParser, sqlToSimple}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.data.admin.{CategoryContents, CategoryHeader, FoodHeader}
import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.foodsql.admin.HeaderRows
import uk.ac.ncl.openlab.intake24.foodsql.{FirstRowValidation, FirstRowValidationClause}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

trait FoodBrowsingAdminQueries
  extends SqlDataService
    with SqlResourceLoader
    with FirstRowValidation
    with HeaderRows {

  private val logger = LoggerFactory.getLogger(classOf[FoodBrowsingAdminQueries])

  lazy private val categoryFoodContentsHeadersQuery = sqlFromResource("admin/category_contents_foods_headers.sql")

  protected def categoryFoodContentsHeadersQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[FoodHeader]] = {
    val validation = SQL("select (select 1 from categories where code={category_code}) as v1, (select 1 from locales where id={locale_id}) as v2")
      .on('category_code -> code, 'locale_id -> locale)
      .executeQuery()
      .as((SqlParser.int(1).? ~ SqlParser.int(2).?).single)

    if (validation._1.isEmpty)
      Left(RecordNotFound(new RuntimeException(s"Category $code does not exist")))
    else if (validation._2.isEmpty)
      Left(RecordNotFound(new RuntimeException(s"Locale $locale does not exist")))
    else {
      Right(SQL(categoryFoodContentsHeadersQuery).on('category_code -> code, 'locale_id -> locale)
        .executeQuery()
        .as(Macro.namedParser[FoodHeaderRow].*)
        .map(_.asFoodHeader))
    }
  }

  lazy private val categoryFoodContentsCodesQuery = sqlFromResource("admin/category_contents_foods_codes.sql")

  protected def categoryFoodContentsCodesQuery(code: String)(implicit conn: java.sql.Connection): Either[LookupError, Set[String]] = {
    val result = SQL(categoryFoodContentsCodesQuery).on('category_code -> code).executeQuery()

    parseWithCategoryValidation(code, result, SqlParser.str("code").+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.toSet)
  }

  lazy private val categorySubcategoryContentsHeadersQuery = sqlFromResource("admin/category_contents_subcategories_headers.sql")

  protected def categorySubcategoryContentsHeadersQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
    val result = SQL(categorySubcategoryContentsHeadersQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(code, result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  lazy private val categorySubcategoryContentsCodesQuery = sqlFromResource("admin/category_contents_subcategories_codes.sql")

  protected def categorySubcategoryContentsCodesQuery(code: String)(implicit conn: java.sql.Connection): Either[LookupError, Set[String]] = {
    val result = SQL(categorySubcategoryContentsCodesQuery).on('category_code -> code).executeQuery()

    parseWithCategoryValidation(code, result, SqlParser.str("code").+)(Seq(FirstRowValidationClause("code", () => Right(List())))).right.map(_.toSet)
  }

  protected def getCategoryContentsQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, CategoryContents] =
    for (
      foods <- categoryFoodContentsHeadersQuery(code, locale).right;
      subcategories <- categorySubcategoryContentsHeadersQuery(code, locale).right
    ) yield CategoryContents(foods, subcategories)

  protected def getFoodParentCategoriesHeadersQuery(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
    val result =
      SQL(
        """|WITH v AS(
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