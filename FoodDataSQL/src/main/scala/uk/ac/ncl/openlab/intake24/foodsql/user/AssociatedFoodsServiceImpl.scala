package uk.ac.ncl.openlab.intake24.foodsql.user

import javax.inject.Inject
import javax.sql.DataSource

import anorm.{Macro, SQL, sqlToSimple}
import com.google.inject.Singleton
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.foodsql.{FirstRowValidation, FirstRowValidationClause}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.AssociatedFoodsService
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

@Singleton
class AssociatedFoodsServiceImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends AssociatedFoodsService with SqlDataService with SqlResourceLoader with FirstRowValidation {

  private case class AssociatedFoodPromptsRow(associated_food_code: Option[String], associated_category_code: Option[String],
                                              text: String, link_as_main: Boolean, generic_name: String, locale_id: String, af_locale_id: String)

  private val getAssociatedFoodsQuery = sqlFromResource("user/get_associated_foods_frv.sql")

  def getAssociatedFoods(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFood]] = tryWithConnection {
    implicit conn =>

      val result = SQL(getAssociatedFoodsQuery).on('food_code -> foodCode, 'locale_id -> locale).executeQuery()

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
          val (local, prototype) = rows.partition(_.af_locale_id == locale)

          if (local.nonEmpty)
            local.map(mkPrompt)
          else
            prototype.map(mkPrompt)
      }
  }
}
