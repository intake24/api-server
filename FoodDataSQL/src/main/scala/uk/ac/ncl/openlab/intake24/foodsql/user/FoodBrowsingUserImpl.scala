package uk.ac.ncl.openlab.intake24.foodsql.user

import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodBrowsingService
import anorm.SqlParser
import uk.ac.ncl.openlab.intake24.UserCategoryContents
import uk.ac.ncl.openlab.intake24.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait FoodBrowsingUserImpl extends FoodBrowsingService with SqlDataService {

  def rootCategories(locale: String): Either[LocaleError, Seq[UserCategoryHeader]] = tryWithConnection {
    implicit conn =>
      val query =
        """|WITH v AS(
           |  SELECT (SELECT id FROM locales WHERE id='en_GB') AS locale_id
           |), t AS(
           |SELECT DISTINCT c.code, c.description, c.is_hidden
           |  FROM categories AS c 
           |    LEFT JOIN categories_categories AS cc1 ON c.code=cc1.subcategory_code
           |  WHERE cc1.category_code IS NULL OR NOT EXISTS(SELECT is_hidden FROM categories_categories AS cc2 INNER JOIN categories AS c2 ON cc2.category_code=c2.code WHERE NOT is_hidden AND cc2.subcategory_code=c.code)
           |)
           |SELECT v.locale_id, t.code, t.description, t.is_hidden, local_description FROM v CROSS JOIN t LEFT JOIN categories_local ON v.locale_id = categories_local.locale_id AND t.code=categories_local.category_code
           |  UNION ALL
           |SELECT v.locale_id, NULL, NULL, NULL, NULL FROM v WHERE NOT EXISTS(SELECT 1 FROM t)
           |ORDER BY local_description""".stripMargin

      val result = SQL(query).on('locale_id -> locale).executeQuery()

      parseWithLocaleValidation(result, Macro.namedParser[CategoryHeaderRow].+)(Seq(FirstRowValidationClause("code", Right(List())))).right.map(_.map(_.asCategoryHeader))
  }
  }

  def categoryContents(code: String, locale: String): Either[FoodCodeError, UserCategoryContents]

  def foodAllCategories(code: String): Seq[String] = tryWithConnection {
    implicit conn =>
      val query = """|WITH RECURSIVE t(code, level) AS (
                   |(SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
                   | UNION ALL
                   |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
                   |)
                   |SELECT code
                   |FROM t 
                   |ORDER BY level""".stripMargin
      SQL(query).on('food_code -> code).executeQuery().as(SqlParser.str("code").*)
  }

  def categoryAllCategories(code: String): Seq[String] = tryWithConnection {
    implicit conn =>
      val query = """|WITH RECURSIVE t(code, level) AS (
                   |(SELECT category_code as code, 0 as level FROM categories_categories WHERE subcategory_code = {category_code} ORDER BY code)
                   | UNION ALL
                   |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
                   |)
                   |SELECT code
                   |FROM t 
                   |ORDER BY level""".stripMargin
      SQL(query).on('category_code -> code).executeQuery().as(SqlParser.str("code").*)
  }

}