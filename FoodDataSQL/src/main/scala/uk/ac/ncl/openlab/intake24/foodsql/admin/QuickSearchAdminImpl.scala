package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import anorm._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.QuickSearchService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait QuickSearchAdminImpl extends QuickSearchService with SqlDataService with HeaderRows {
  
  def searchFoods(searchTerm: String, locale: String): Either[DatabaseError, Seq[FoodHeader]] = tryWithConnection {
    implicit conn =>
      val lowerCaseTerm = searchTerm.toLowerCase

      val query =
        """|SELECT code, description, local_description, do_not_use
           |FROM foods LEFT JOIN foods_local ON foods.code = foods_local.food_code 
           |WHERE (lower(local_description) LIKE {pattern} OR lower(description) LIKE {pattern} OR lower(code) LIKE {pattern})
           |AND foods_local.locale_id = {locale_id}
           |ORDER BY local_description DESC
           |LIMIT 30""".stripMargin

      Right(SQL(query).on('pattern -> s"%${lowerCaseTerm}%", 'locale_id -> locale).executeQuery().as(Macro.namedParser[FoodHeaderRow].*).map(_.asFoodHeader))
  }

  def searchCategories(searchTerm: String, locale: String): Either[DatabaseError, Seq[CategoryHeader]] = tryWithConnection {
    implicit conn =>
      val lowerCaseTerm = searchTerm.toLowerCase

      val query =
        """|SELECT code, description, local_description, is_hidden
           |FROM categories LEFT JOIN categories_local ON categories.code = categories_local.category_code
           |WHERE (lower(local_description) LIKE {pattern} OR lower(description) LIKE {pattern} OR lower(code) LIKE {pattern})
           |AND categories_local.locale_id = {locale_id}
           |ORDER BY local_description DESC
           |LIMIT 30""".stripMargin

      Right(SQL(query).on('pattern -> s"%${lowerCaseTerm}%", 'locale_id -> locale).executeQuery().as(Macro.namedParser[CategoryHeaderRow].*).map(_.asCategoryHeader))

  }
}
