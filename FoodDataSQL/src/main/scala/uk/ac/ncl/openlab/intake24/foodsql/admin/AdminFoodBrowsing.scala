package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.FoodHeader
import anorm._
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.CategoryContents

import anorm.NamedParameter.symbol

import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService

trait AdminFoodBrowsing extends SqlDataService with AdminHeaderRows {
 def uncategorisedFoods(locale: String): Seq[FoodHeader] = tryWithConnection {
    implicit conn =>
      val query =
        """|SELECT code, description, local_description, do_not_use
           |FROM foods LEFT JOIN foods_local ON foods.code = foods_local.food_code
           |           LEFT JOIN foods_categories ON foods.code = foods_categories.food_code
           |WHERE category_code IS NULL""".stripMargin

      SQL(query).executeQuery().as(Macro.namedParser[FoodHeaderRow].*).map(_.asFoodHeader)
  }


  def rootCategories(locale: String): Seq[CategoryHeader] = tryWithConnection {
    implicit conn =>
      val query =
        """|SELECT code, description, local_description, is_hidden 
           |FROM categories 
           |  LEFT JOIN categories_categories 
           |    ON categories.code = categories_categories.subcategory_code
           |  LEFT JOIN categories_local
           |    ON categories.code = categories_local.category_code AND categories_local.locale_id = {locale_id} 
           |WHERE categories_categories.category_code IS NULL
           |ORDER BY description""".stripMargin

      SQL(query).on('locale_id -> locale).executeQuery().as(Macro.namedParser[CategoryHeaderRow].*).map(_.asCategoryHeader)
  }


  def categoryContents(code: String, locale: String): CategoryContents = tryWithConnection {
    implicit conn =>
      val foodsQuery =
        """|SELECT code, description, local_description, do_not_use 
           |FROM foods_categories 
           |  INNER JOIN foods ON foods.code = foods_categories.food_code 
           |  LEFT JOIN foods_local ON foods.code = foods_local.food_code AND foods_local.locale_id = {locale_id}
           |WHERE category_code = {category_code}
           |ORDER BY local_description""".stripMargin

      val foods = SQL(foodsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery().as(Macro.namedParser[FoodHeaderRow].*).map(_.asFoodHeader)

      val categoriesQuery =
        """|SELECT code, description, local_description, is_hidden
           |FROM categories_categories 
           |     INNER JOIN categories ON categories.code = categories_categories.subcategory_code 
           |     LEFT JOIN categories_local ON categories.code = categories_local.category_code AND categories_local.locale_id = {locale_id}
           |WHERE categories_categories.category_code = {category_code}
           |ORDER BY local_description""".stripMargin

      val categories = SQL(categoriesQuery).on('category_code -> code, 'locale_id -> locale).executeQuery().as(Macro.namedParser[CategoryHeaderRow].*).map(_.asCategoryHeader)

      CategoryContents(foods, categories)
  }
  
   def foodParentCategories(code: String, locale: String): Seq[CategoryHeader] = tryWithConnection {
    implicit conn =>
      SQL("""|SELECT code, description, local_description, is_hidden 
             |FROM foods_categories 
             |     JOIN categories ON foods_categories.category_code = code
             |     LEFT JOIN categories_local ON categories_local.category_code = code AND categories_local.locale_id = {locale_id} 
             |WHERE food_code = {food_code}
             |ORDER BY foods_categories.category_code""".stripMargin)
        .on('food_code -> code, 'locale_id -> locale)
        .executeQuery()
        .as(Macro.namedParser[CategoryHeaderRow].*)
        .map(_.asCategoryHeader)
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