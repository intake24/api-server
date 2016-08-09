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
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalFoodCodeError
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalCategoryCodeError

trait AdminFoodBrowsing extends SqlDataService with FoodBrowsingAdminService with AdminHeaderRows {
  def uncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]] = tryWithConnection {
    implicit conn =>
      val query =
        """|WITH v AS(
           |  SELECT (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
           |), t(code, description) AS(
           |  SELECT code, description FROM foods WHERE NOT EXISTS(SELECT 1 FROM foods_categories WHERE food_code=foods.code)
           |)
           |SELECT v.locale_id, code, description, local_description, do_not_use
           |  FROM v CROSS JOIN t
           |    LEFT JOIN foods_local ON foods_local.food_code=t.code AND foods_local.locale_id=v.locale_id
           |UNION ALL
           |SELECT v.locale_id, NULL, NULL, NULL, NULL FROM v WHERE NOT EXISTS(SELECT 1 FROM t)
           |ORDER BY local_description""".stripMargin

      val result = SQL(query).on('locale_id -> locale).executeQuery()

      parseWithLocaleValidation(result, Macro.namedParser[FoodHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asFoodHeader))
  }

  def rootCategories(locale: String): Either[LocaleError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      val query =
        """|WITH v AS(
           |  SELECT (SELECT id FROM locales WHERE id='en_GB') AS locale_id
           |), t AS(
           |SELECT DISTINCT c.code, c.description, c.is_hidden
           |  FROM categories AS c 
           |    LEFT JOIN categories_categories AS cc1 ON c.code=cc1.subcategory_code
           |WHERE cc1.category_code IS NULL OR NOT EXISTS(SELECT is_hidden FROM categories_categories AS cc2 INNER JOIN categories AS c2 ON cc2.category_code=c2.code WHERE NOT is_hidden AND cc2.subcategory_code=c.code)
           |)
           |SELECT v.locale_id, t.code, t.description, t.is_hidden, local_description FROM v CROSS JOIN t LEFT JOIN categories_local ON v.locale_id = categories_local.locale_id AND t.code=categories_local.category_code
           |  UNION ALL
           |SELECT v.locale_id, NULL, NULL, NULL, NULL FROM v WHERE NOT EXISTS(SELECT 1 FROM t)
           |ORDER BY local_description""".stripMargin

      val result = SQL(query).on('locale_id -> locale).executeQuery()

      parseWithLocaleValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  def categoryFoodContents(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalCategoryCodeError, Seq[FoodHeader]] = {
    val foodsQuery =
      """|WITH v AS(
           |  SELECT (SELECT code FROM categories WHERE code={category_code}) AS category_code,
           |         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
           |)
           |SELECT v.locale_id, v.category_code, code, description, local_description, do_not_use
           |FROM 
           |v LEFT JOIN foods_categories ON foods_categories.category_code = v.category_code
           |  LEFT JOIN foods ON foods.code = foods_categories.food_code 
           |  LEFT JOIN foods_local ON foods.code = foods_local.food_code AND foods_local.locale_id = v.locale_id
           |ORDER BY local_description""".stripMargin

    val result = SQL(foodsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(result, Macro.namedParser[FoodHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asFoodHeader))
  }

  def categorySubcategoryContents(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalCategoryCodeError, Seq[CategoryHeader]] = {
    val categoriesQuery =
      """|WITH v AS(
           |  SELECT (SELECT code FROM categories WHERE code='WTBI') AS category_code,
           |         (SELECT id FROM locales WHERE id='en_GB') AS locale_id
           |)
           |SELECT v.locale_id, v.category_code, subcategory_code AS code, description, local_description, is_hidden
           |FROM 
           |  v LEFT JOIN categories_categories ON categories_categories.category_code = v.category_code
           |  LEFT JOIN categories ON categories.code = categories_categories.subcategory_code 
           |  LEFT JOIN categories_local ON categories.code = categories_local.category_code AND categories_local.locale_id = v.locale_id
           |ORDER BY local_description""".stripMargin

    val result = SQL(categoriesQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  def categoryContents(code: String, locale: String): Either[LocalCategoryCodeError, CategoryContents] = tryWithConnection {
    implicit conn =>
      categoryFoodContents(code, locale).right.flatMap {
        foods =>
          categorySubcategoryContents(code, locale).right.flatMap {
            subcategories =>
              Right(CategoryContents(foods, subcategories))
          }
      }
  }

  def foodParentCategories(code: String, locale: String): Either[LocalFoodCodeError, Seq[CategoryHeader]] = tryWithConnection {
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

  def foodAllCategories(code: String): Seq[String] = tryWithConnection {
    implicit conn =>
      val query =
        """|WITH RECURSIVE t(code, level) AS (
           |(SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
           | UNION ALL
           |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
           |)
           |SELECT code 
           |FROM t 
           |ORDER BY level""".stripMargin

      SQL(query)
        .on('food_code -> code)
        .executeQuery()
        .as(SqlParser.str("code").*)
  }

  def foodAllCategories(code: String, locale: String): Seq[CategoryHeader] = tryWithConnection {
    implicit conn =>
      val query =
        """|WITH RECURSIVE t(code, level) AS (
           |(SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
           | UNION ALL
           |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
           |)
           |SELECT categories.code, description, local_description, is_hidden 
           |FROM t 
           |    JOIN categories on t.code = categories.code
           |    LEFT JOIN categories_local on t.code = categories_local.category_code AND categories_local.locale_id = {locale_id}
           |ORDER BY level""".stripMargin

      SQL(query)
        .on('food_code -> code, 'locale_id -> locale)
        .executeQuery()
        .as(Macro.namedParser[CategoryHeaderRow].*)
        .map(_.asCategoryHeader)
  }

  def categoryParentCategories(code: String, locale: String): Seq[CategoryHeader] = tryWithConnection {
    implicit conn =>
      SQL("""|SELECT code, description, local_description, is_hidden 
             |FROM categories_categories 
             |     JOIN categories ON categories_categories.category_code = code 
             |     LEFT JOIN categories_local ON categories_local.category_code = code AND categories_local.locale_id = {locale_id}
             |WHERE subcategory_code = {category_code}
             |ORDER BY categories_categories.category_code""".stripMargin)
        .on('category_code -> code, 'locale_id -> locale)
        .executeQuery()
        .as(Macro.namedParser[CategoryHeaderRow].*)
        .map(_.asCategoryHeader)
  }

  def categoryAllCategories(code: String): Seq[String] = tryWithConnection {
    implicit conn =>
      val query =
        """|WITH RECURSIVE t(code, level) AS (
           |(SELECT category_code AS code, 0 AS level FROM categories_categories WHERE subcategory_code = {category_code} ORDER BY code)
           | UNION ALL
           |(SELECT category_code AS code, level + 1 AS level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
           |)
           |SELECT code 
           |FROM t 
           |ORDER BY level""".stripMargin

      SQL(query)
        .on('category_code -> code)
        .executeQuery()
        .as(SqlParser.str("code").*)
  }

  def categoryAllCategories(code: String, locale: String): Seq[CategoryHeader] = tryWithConnection {
    implicit conn =>
      val query =
        """|WITH RECURSIVE t(code, level) AS (
           |(SELECT category_code AS code, 0 AS level FROM categories_categories WHERE subcategory_code = {category_code} ORDER BY code)
           | UNION ALL
           |(SELECT category_code AS code, level + 1 AS level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
           |)
           |SELECT categories.code, description, local_description, is_hidden 
           |FROM t 
           |     JOIN categories on t.code = categories.code
           |     LEFT JOIN categories_local on t.code = categories_local.category_code AND categories_local.locale_id = {locale_id}
           |ORDER BY level""".stripMargin

      SQL(query)
        .on('category_code -> code, 'locale_id -> locale)
        .executeQuery()
        .as(Macro.namedParser[CategoryHeaderRow].*)
        .map(_.asCategoryHeader)
  }
}