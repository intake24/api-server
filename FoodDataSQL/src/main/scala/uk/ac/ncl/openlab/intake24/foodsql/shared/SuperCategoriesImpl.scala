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

trait SuperCategoriesImpl extends FirstRowValidation with HeaderRows {
  protected def foodAllCategoriesImpl(code: String)(implicit conn: java.sql.Connection): Either[LookupError, Seq[String]] = {
    val query =
      """|WITH RECURSIVE t(code, level) AS (
         |  (SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code={food_code} ORDER BY code)
         |  UNION ALL
         |  (SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
         |)
         |SELECT (SELECT code FROM foods WHERE code={food_code}) AS food_code, code, level FROM t
         |UNION ALL
         |SELECT (SELECT code FROM foods WHERE code={food_code}), NULL, NULL WHERE NOT EXISTS(SELECT 1 FROM foods_categories WHERE food_code={food_code})
         |ORDER BY level""".stripMargin

    val result = SQL(query).on('food_code -> code).executeQuery()

    parseWithFoodValidation(result, SqlParser.str("code").+)(Seq(FirstRowValidationClause("code", Right(List()))))
  }

  protected def foodAllCategoriesImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
    val query =
      """|WITH RECURSIVE t(code, level) AS (
         |(SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
         | UNION ALL
         |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
         |)
         |SELECT (SELECT code FROM foods WHERE code={food_code}) as food_code, (SELECT id FROM locales WHERE id={locale_id}) AS locale_id, categories.code, description, local_description, is_hidden 
         |FROM t 
         | JOIN categories on t.code = categories.code
         | LEFT JOIN categories_local on t.code = categories_local.category_code AND categories_local.locale_id = {locale_id}
         |UNION ALL
         |SELECT (SELECT code FROM foods WHERE code={food_code}), (SELECT id FROM locales WHERE id={locale_id}), NULL, NULL, NULL, NULL WHERE NOT EXISTS(SELECT 1 FROM foods_categories WHERE food_code={food_code})""".stripMargin

    val result = SQL(query).on('food_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndFoodValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }

  def categoryAllCategoriesImpl(code: String)(implicit conn: java.sql.Connection): Either[LookupError, Seq[String]] = {
    val query =
      """|WITH RECURSIVE t(code, level) AS (
         |(SELECT category_code as code, 0 as level FROM categories_categories WHERE subcategory_code = {category_code} ORDER BY code)
         | UNION ALL
         |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
         |)
         |SELECT (SELECT code FROM categories WHERE code={category_code}) as category_code, (SELECT id FROM locales WHERE id={locale_id}) AS locale_id, categories.code, description, local_description, is_hidden 
         |FROM t 
         | JOIN categories on t.code = categories.code
         | LEFT JOIN categories_local on t.code = categories_local.category_code AND categories_local.locale_id = {locale_id}
         |UNION ALL
         |SELECT (SELECT code FROM categories WHERE code={category_code}), (SELECT id FROM locales WHERE id={locale_id}), NULL, NULL, NULL, NULL WHERE NOT EXISTS(SELECT 1 FROM categories_categories WHERE subcategory_code={category_code})""".stripMargin

    val result = SQL(query).on('category_code -> code).executeQuery()

    parseWithCategoryValidation(result, SqlParser.str("code").+)(Seq(FirstRowValidationClause("code", Right(List()))))
  }

  def categoryAllCategoriesImpl(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[CategoryHeader]] = {
    val query =
      """|WITH RECURSIVE t(code, level) AS (
         |(SELECT category_code as code, 0 as level FROM categories_categories WHERE subcategory_code = {category_code} ORDER BY code)
         | UNION ALL
         |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
         |)
         |SELECT (SELECT code FROM categories WHERE code={category_code}) as food_code, (SELECT id FROM locales WHERE id={locale_id}) AS locale_id, categories.code, description, local_description, is_hidden 
         |FROM t 
         | JOIN categories on t.code = categories.code
         | LEFT JOIN categories_local on t.code = categories_local.category_code AND categories_local.locale_id = {locale_id}
         |UNION ALL
          |SELECT (SELECT code FROM categories WHERE code={category_code}), (SELECT id FROM locales WHERE id={locale_id}), NULL, NULL, NULL, NULL WHERE NOT EXISTS(SELECT 1 FROM categories_categories WHERE subcategory_code={category_code})""".stripMargin

    val result = SQL(query).on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }
}
