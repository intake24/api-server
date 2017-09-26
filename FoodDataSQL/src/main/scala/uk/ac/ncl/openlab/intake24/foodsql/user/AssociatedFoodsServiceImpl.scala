package uk.ac.ncl.openlab.intake24.foodsql.user

import javax.inject.Inject
import javax.sql.DataSource

import anorm.{Macro, SQL, sqlToSimple}
import com.google.inject.Singleton
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.foodsql.modular.{FoodQueries, LocaleQueries}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.AssociatedFoodsService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

@Singleton
class AssociatedFoodsServiceImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends
  AssociatedFoodsService with SqlDataService with LocaleQueries with FoodQueries {

  private case class AssociatedFoodPromptsRow(associated_food_code: Option[String], associated_category_code: Option[String],
                                              text: String, link_as_main: Boolean, generic_name: String) {
    def asAssociatedFood = {
      val foodOrCategory: Either[String, String] = (associated_food_code, associated_category_code) match {
        case (Some(foodCode), None) => Left(foodCode)
        case (None, Some(categoryCode)) => Right(categoryCode)
        case _ => throw new RuntimeException(s"Unexpected associated food row format: ${toString()}")
      }

      AssociatedFood(foodOrCategory, text, link_as_main, generic_name)
    }
  }

  private val getAssociatedFoodsQuery =
    """SELECT associated_food_code, associated_category_code, text, link_as_main, generic_name
      |FROM associated_foods WHERE food_code={food_code} AND locale_id={locale_id} ORDER BY id""".stripMargin

  def getAssociatedFoodsForLocaleQuery(foodCode: String, localeId: String)(implicit connection: java.sql.Connection): Either[UnexpectedDatabaseError, Seq[AssociatedFood]] = {
    Right(SQL(getAssociatedFoodsQuery)
      .on('locale_id -> localeId, 'food_code -> foodCode)
      .executeQuery()
      .as(Macro.namedParser[AssociatedFoodPromptsRow].*)
      .map(_.asAssociatedFood))
  }

  def getAssociatedFoods(foodCode: String, localeId: String): Either[LocalLookupError, Seq[AssociatedFood]] = tryWithConnection {
    implicit conn =>
      withTransaction {

        getLocaleQuery(localeId).flatMap {
          localeRecord =>
            validateFoodCodeQuery(foodCode).flatMap {
              _ =>
                getAssociatedFoodsForLocaleQuery(foodCode, localeId).flatMap {
                  localAssociatedFoods =>
                    if (localAssociatedFoods.nonEmpty)
                      Right(localAssociatedFoods)
                    else
                      localeRecord.prototypeLocale match {
                        case Some(prototypeLocaleId) =>
                          isTranslationRequiredQuery(localeId).flatMap {
                            translationRequired =>
                              if (translationRequired)
                                Right(Seq())
                              else
                                getAssociatedFoodsForLocaleQuery(foodCode, prototypeLocaleId)
                          }
                        case None =>
                          Right(Seq())
                      }
                }
            }
        }
      }
  }

}
