package uk.ac.ncl.openlab.intake24.foodsql.user

import scala.Left
import scala.Right

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.services.CodeError

trait UserAssociatedFoodsImpl extends SqlDataService {

  private case class AssociatedFoodPromptsRow(associated_food_code: Option[String], associated_category_code: Option[String],
    text: Option[String], link_as_main: Option[Boolean], generic_name: Option[String], locale_id: Option[String])

  private val query =
    """|SELECT associated_food_code, associated_category_code, text, link_as_main, generic_name, locale_id
       |  FROM foods LEFT JOIN associated_foods ON foods.code = associated_foods.food_code
       |WHERE
       |  foods.code = {food_code} 
       |  AND (associated_foods.locale_id = {locale_id} OR associated_foods.locale_id IS NULL OR associated_foods.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = 'locale_id'))
       |ORDER BY id""".stripMargin

  def associatedFoods(foodCode: String, locale: String): Either[CodeError, Seq[AssociatedFood]] = tryWithConnection {
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
        Left(CodeError.UndefinedCode)
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