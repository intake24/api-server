package uk.ac.ncl.openlab.intake24.foodsql.admin

import scala.Left
import scala.Right
import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.FoodHeader

import anorm.NamedParameter

import org.postgresql.util.PSQLException

import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService

import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader
import java.sql.Connection
import uk.ac.ncl.openlab.intake24.foodsql.SimpleValidation
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentCreateError
import anorm.SqlParser
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.foodsql.user.AssociatedFoodsUserImpl
import javax.sql.DataSource
import com.google.inject.Inject
import com.google.inject.name.Named

class AssociatedFoodsAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends AssociatedFoodsAdminImpl

trait AssociatedFoodsAdminImpl extends AssociatedFoodsAdminService with AssociatedFoodsUserImpl with SqlDataService with SqlResourceLoader with SimpleValidation {

  private val logger = LoggerFactory.getLogger(classOf[AssociatedFoodsAdminImpl])

  private case class AssociatedFoodPromptsRow(
    associated_food_code: Option[String], food_english_description: Option[String], food_local_description: Option[String], food_do_not_use: Option[Boolean],
    associated_category_code: Option[String], category_english_description: Option[String], category_local_description: Option[String], category_is_hidden: Option[Boolean],
    text: Option[String], link_as_main: Option[Boolean], generic_name: Option[String])

  private lazy val getAssociatedFoodsQuery = sqlFromResource("admin/get_associated_foods.sql")

  def getAssociatedFoodsWithHeaders(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFoodWithHeader]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        validateFoodAndLocale(foodCode, locale).right.flatMap {
          _ =>
            val rows = SQL(getAssociatedFoodsQuery).on('food_code -> foodCode, 'locale_id -> locale).executeQuery().as(Macro.namedParser[AssociatedFoodPromptsRow].*)

            Right(rows.map {
              row =>
                val foodOrCategory: Either[FoodHeader, CategoryHeader] =
                  if (row.food_english_description.nonEmpty)
                    Left(FoodHeader(row.associated_food_code.get, row.food_english_description.get, row.food_local_description, row.food_do_not_use))
                  else
                    Right(CategoryHeader(row.associated_category_code.get, row.category_english_description.get, row.category_local_description, row.category_is_hidden.get))

                AssociatedFoodWithHeader(foodOrCategory, row.text.get, row.link_as_main.get, row.generic_name.get)

            })
        }
      }
  }

  def updateAssociatedFoods(foodCode: String, locale: String, foods: Seq[AssociatedFood]): Either[LocalLookupError, Unit] = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)

      SQL("DELETE FROM associated_food_prompts WHERE food_code={food_code} AND locale_id={locale_id}")
        .on('food_code -> foodCode, 'locale_id -> locale)
        .execute()

      if (foods.nonEmpty) {

        val params = foods.map {
          p =>

            val foodOption = p.foodOrCategoryCode.left.toOption
            val categoryOption = p.foodOrCategoryCode.right.toOption

            Seq[NamedParameter]('food_code -> foodCode, 'locale_id -> locale, 'associated_food_code -> foodOption,
              'associated_category_code -> categoryOption, 'text -> p.promptText, 'link_as_main -> p.linkAsMain, 'generic_name -> p.genericName)
        }

        batchSql("INSERT INTO associated_food_prompts VALUES (DEFAULT, {food_code}, {locale_id}, {associated_category_code}, {associated_food_code}, {text}, {link_as_main}, {generic_name})", params)
          .execute()
      }

      conn.commit()

      Right(())
  }

  def deleteAllAssociatedFoods(locale: String): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      logger.debug("Deleting existing associated food prompts")

      SQL("DELETE FROM associated_foods WHERE locale_id={locale_id}").on('locale_id -> locale).execute()

      Right(())
  }

  def createAssociatedFoods(assocFoods: Map[String, Seq[AssociatedFood]], locale: String): Either[LocalDependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      val promptParams = assocFoods.flatMap {
        case (foodCode, foods) =>
          foods.map {
            assocFood =>
              Seq[NamedParameter]('food_code -> foodCode, 'locale_id -> locale, 'associated_food_code -> assocFood.foodOrCategoryCode.left.toOption, 'associated_category_code -> assocFood.foodOrCategoryCode.right.toOption,
                'text -> assocFood.promptText, 'link_as_main -> assocFood.linkAsMain, 'generic_name -> assocFood.genericName)
          }
      }.toSeq

      if (promptParams.nonEmpty) {

        logger.debug("Writing " + assocFoods.values.map(_.size).foldLeft(0)(_ + _) + " associated food prompts to database")

        val constraintErrors = Map(
          "associated_food_prompts_assoc_category_fk" -> ParentRecordNotFound,
          "associated_food_prompts_assoc_food_fk" -> ParentRecordNotFound,
          "associated_food_prompts_food_code_fk" -> ParentRecordNotFound,
          "associated_food_prompts_locale_id_fk" -> UndefinedLocale)

        tryWithConstraintsCheck(constraintErrors) {
          conn.setAutoCommit(false)
          batchSql("""INSERT INTO associated_foods VALUES (DEFAULT, {food_code}, {locale_id}, {associated_food_code}, {associated_category_code}, {text}, {link_as_main}, {generic_name})""", promptParams).execute()
          conn.commit()
          Right(())
        }
      } else
        Right(())
  }
}
