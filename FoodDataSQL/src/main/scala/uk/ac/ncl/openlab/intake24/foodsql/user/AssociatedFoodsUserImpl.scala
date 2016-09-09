package uk.ac.ncl.openlab.intake24.foodsql.user

import scala.Left
import scala.Right

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.user.AssociatedFoodsService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation

trait AssociatedFoodsUserImpl extends AssociatedFoodsService with SqlDataService with FirstRowValidation {

  private case class AssociatedFoodPromptsRow(associated_food_code: Option[String], associated_category_code: Option[String],
    text: String, link_as_main: Boolean, generic_name: String, locale_id: String)

  private val query =
    """|WITH v AS(
       |  SELECT (SELECT code FROM foods WHERE code = {food_code}) AS food_code, 
       |  SELECT (SELECT id FROM locales WHERE id = {locale_id}) AS locale_id
       |)
       |SELECT v.food_code, v.locale_id, associated_foods.id, associated_food_code, associated_category_code, text, link_as_main, generic_name
       |FROM v LEFT JOIN associated_foods 
       |   ON v.food_code = associated_foods.food_code 
       |      AND (v.locale_id = associated_foods.locale_id OR associated_foods.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = v.locale_id))
       |ORDER BY id""".stripMargin

  def getAssociatedFoods(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFood]] = tryWithConnection {
    implicit conn =>

      val result = SQL(query).on('food_code -> foodCode, 'locale_id -> locale).executeQuery()

      val parser = Macro.namedParser[AssociatedFoodPromptsRow].+

      def mkPrompt(row: AssociatedFoodPromptsRow) = {

        val foodOrCategory: Either[String, String] = (row.associated_food_code, row.associated_category_code) match {
          case (Some(foodCode), None) => Left(foodCode)
          case (None, Some(categoryCode)) => Right(categoryCode)
          case _ => throw new RuntimeException(s"Unexpected associated food row format: ${row.toString()}")
        }

        AssociatedFood(foodOrCategory, row.text, row.link_as_main, row.generic_name)
      }

      parseWithLocaleAndFoodValidation(foodCode, result, parser)(Seq(FirstRowValidationClause("id", () => Right(List())))).right.map {
        rows =>
          val (local, prototype) = rows.partition(_.locale_id == locale)

          if (local.nonEmpty)
            local.map(mkPrompt)
          else
            prototype.map(mkPrompt)
      }
  }
}
