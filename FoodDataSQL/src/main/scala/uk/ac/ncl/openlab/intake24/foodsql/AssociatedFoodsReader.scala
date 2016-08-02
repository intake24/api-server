package uk.ac.ncl.openlab.intake24.foodsql

import scala.Left
import scala.Right

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.UserAssociatedFood
import uk.ac.ncl.openlab.intake24.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.UserFoodHeader
import uk.ac.ncl.openlab.intake24.services.CodeError

trait AssociatedFoodsReader extends SqlDataService {

  private case class AssociatedFoodPromptsRow(associated_food_code: Option[String], associated_food_description: Option[String],
    associated_category_code: Option[String], associated_category_description: Option[String], text: Option[String],
    link_as_main: Option[Boolean], generic_name: Option[String], locale_id: Option[String])

  private def buildQuery(includeInherited: Boolean) =
    s"""|SELECT associated_food_code, foods_local.local_description as associated_food_description,
        |  associated_category_code, categories_local.local_description as associated_category_description, 
        |  text, link_as_main, generic_name, associated_foods.locale_id
        |FROM foods
        |  LEFT JOIN associated_foods ON foods.code = associated_foods.food_code
	      |  LEFT JOIN categories_local ON associated_foods.associated_category_code = categories_local.category_code AND categories_local.locale_id = {locale_id}
		    |  LEFT JOIN foods_local ON associated_foods.associated_food_code = foods_local.food_code AND foods_local.locale_id = {locale_id}
        |WHERE
        |  foods.code = {food_code} 
        |  AND ${ if(includeInherited) 
              "(associated_foods.locale_id = {locale_id} OR associated_foods.locale_id IS NULL OR associated_foods.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id}))"
              else
              "associated_foods.locale_id = {locale_id}" }
        |ORDER BY id""".stripMargin

  private val queryWithInherited = buildQuery(true)
  
  private val queryWithoutInherited = buildQuery(false) 

  def associatedFoodsImpl(foodCode: String, locale: String, includeInherited: Boolean): Either[CodeError, Seq[UserAssociatedFood]] = tryWithConnection {
    implicit conn =>

      val rows = SQL(if (includeInherited) queryWithInherited else queryWithoutInherited).on('food_code -> foodCode, 'locale_id -> locale).executeQuery().as(Macro.namedParser[AssociatedFoodPromptsRow].*)

      def mkPrompt(row: AssociatedFoodPromptsRow) = {

        val foodOrCategory: Either[UserFoodHeader, UserCategoryHeader] = (row.associated_food_code, row.associated_food_description, row.associated_category_code, row.associated_category_description) match {
          case ((Some(foodCode), Some(foodDescription), None, None)) => Left(UserFoodHeader(foodCode, foodDescription))
          case (None, None, Some(categoryCode), Some(categoryDescription)) => Right(UserCategoryHeader(categoryCode, categoryDescription))
          case _ => throw new RuntimeException(s"Unexpected associated food row format: ${row.toString()}")
        }

        UserAssociatedFood(foodOrCategory, row.text.get, row.link_as_main.get, row.generic_name.get)
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