package uk.ac.ncl.openlab.intake24.foodsql.modular

import anorm.NamedParameter.symbol
import anorm.{Macro, NamedParameter, SQL, sqlToSimple}
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{LocaleOrParentError, ParentRecordNotFound, UndefinedLocale}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}
import uk.ac.ncl.openlab.intake24.{AssociatedFood, AssociatedFoodWithHeader, CategoryHeader, FoodHeader}

trait AssociatedFoodsAdminQueries extends SqlDataService with SqlResourceLoader {

  private val logger = LoggerFactory.getLogger(classOf[AssociatedFoodsAdminQueries])

  private case class AssociatedFoodPromptsRow(
                                               associated_food_code: Option[String], food_english_description: Option[String], food_local_description: Option[String], food_do_not_use: Option[Boolean],
                                               associated_category_code: Option[String], category_english_description: Option[String], category_local_description: Option[String], category_is_hidden: Option[Boolean],
                                               text: Option[String], link_as_main: Option[Boolean], generic_name: Option[String])

  private lazy val getAssociatedFoodsQuery = sqlFromResource("admin/get_associated_foods.sql")

  protected def getAssociatedFoodsWithHeadersQuery(foodCode: String, locale: String)(implicit conn: java.sql.Connection): Either[Nothing, Seq[AssociatedFoodWithHeader]] = {
    val rows = SQL(getAssociatedFoodsQuery).on('food_code -> foodCode, 'locale_id -> locale).executeQuery().as(Macro.namedParser[AssociatedFoodPromptsRow].*)

    Right(rows.map {
      row =>
        val foodOrCategory: Either[FoodHeader, CategoryHeader] =
          if (row.food_english_description.nonEmpty)
            Left(FoodHeader(row.associated_food_code.get, row.food_english_description.get, row.food_local_description, row.food_do_not_use.getOrElse(false)))
          else
            Right(CategoryHeader(row.associated_category_code.get, row.category_english_description.get, row.category_local_description, row.category_is_hidden.get))

        AssociatedFoodWithHeader(foodOrCategory, row.text.get, row.link_as_main.get, row.generic_name.get)

    })
  }

  protected def deleteAllAssociatedFoodsQuery(locale: String)(implicit conn: java.sql.Connection): Either[Nothing, Unit] = {
    logger.debug("Deleting existing associated food prompts")
    SQL("DELETE FROM associated_foods WHERE locale_id={locale_id}").on('locale_id -> locale).execute()
    Right(())
  }

  protected def deleteAssociatedFoodsQuery(foodCode: String, locale: String)(implicit conn: java.sql.Connection): Either[Nothing, Unit] = {
    SQL("DELETE FROM associated_foods WHERE food_code={food_code} AND locale_id={locale_id}").on('food_code -> foodCode, 'locale_id -> locale).execute()
    Right(())
  }

  protected def createAssociatedFoodsQuery(assocFoods: Map[String, Seq[AssociatedFood]], locale: String)(implicit conn: java.sql.Connection): Either[LocaleOrParentError, Unit] = {
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

      val constraintErrors = Map[String, PSQLException => LocaleOrParentError](
        "associated_food_prompts_assoc_category_fk" -> (e => ParentRecordNotFound(e)),
        "associated_food_prompts_assoc_food_fk" -> (e => ParentRecordNotFound(e)),
        "associated_food_prompts_food_code_fk" -> (e => ParentRecordNotFound(e)),
        "associated_food_prompts_locale_id_fk" -> (e => UndefinedLocale(e)))

      tryWithConstraintsCheck(constraintErrors) {
        batchSql("""INSERT INTO associated_foods VALUES (DEFAULT, {food_code}, {locale_id}, {associated_food_code}, {associated_category_code}, {text}, {link_as_main}, {generic_name})""", promptParams).execute()
        Right(())
      }
    } else
      Right(())
  }
}
