package uk.ac.ncl.openlab.intake24.foodsql.user

import scala.Left
import scala.Right

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedCode
import uk.ac.ncl.openlab.intake24.services.fooddb.user.AssociatedFoodsService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalFoodCodeError

trait AssociatedFoodsUserImpl extends AssociatedFoodsService with SqlDataService {

  private case class AssociatedFoodPromptsRow(associated_food_code: Option[String], associated_category_code: Option[String],
    text: Option[String], link_as_main: Option[Boolean], generic_name: Option[String], locale_id: Option[String])

  private val query =
    """|WITH v AS(
       |  SELECT (SELECT code FROM foods WHERE code = 'AABL') AS food_code, 
       |  SELECT (SELECT id FROM locales WHERE id = 'pt_PT') AS locale_id
       |)
       |SELECT v.food_code, v.locale_id, associated_foods.id, associated_food_code, associated_category_code, text, link_as_main, generic_name
       |FROM v LEFT JOIN associated_foods 
       |   ON v.food_code = associated_foods.food_code 
       |      AND (v.locale_id = associated_foods.locale_id OR associated_foods.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = v.locale_id))
       |ORDER BY id""".stripMargin

  def associatedFoods(foodCode: String, locale: String): Either[LocalFoodCodeError, Seq[AssociatedFood]] = tryWithConnection {
    implicit conn =>

      val rows = SQL(query).on('food_code -> foodCode, 'locale_id -> locale).executeQuery().as(Macro.namedParser[AssociatedFoodPromptsRow].*)

      def mkPrompt(row: AssociatedFoodPromptsRow) = {

        val foodOrCategory: Either[String, String] = (row.associated_food_code, row.associated_category_code) match {
          case (Some(foodCode), None) => Left(foodCode)
          case (None, Some(categoryCode)) => Right(categoryCode)
          case _ => throw new RuntimeException(s"Unexpected associated food row format: ${row.toString()}")
        }

        AssociatedFood(foodOrCategory, row.text.get, row.link_as_main.get, row.generic_name.get)
      }

      if (rows.isEmpty) // No such food in foods table
        Left(UndefinedCode)
      else if (rows.head.text.isEmpty) // All columns will be null if there are no matching associated food records, check
        // the first one that cannot be null
        Right(Seq())
      else {
        val (local, prototype) = rows.partition(_.locale_id.get == locale)

        if (local.nonEmpty)
          Right(local.map(mkPrompt))
        else
          Right(prototype.map(mkPrompt))
      }
  }
}
