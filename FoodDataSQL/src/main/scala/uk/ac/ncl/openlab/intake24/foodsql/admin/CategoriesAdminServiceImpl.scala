package uk.ac.ncl.openlab.intake24.foodsql.admin

import java.util.UUID

import uk.ac.ncl.openlab.intake24.CategoryRecord
import anorm._
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import org.postgresql.util.PSQLException

import anorm.NamedParameter.symbol
import scala.Left
import scala.Right

import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.foodsql.Util

import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CategoryCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalCategoryCodeError
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.VersionConflict
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedCode
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService

trait CategoriesAdminServiceImpl extends SqlDataService with CategoriesAdminService with AdminPortionSizeShared with AdminErrorMessagesShared {

  private def categoryCodeFkConstraintFailedMessage(categoryCode: String) =
    s"Category code $categoryCode is not defined. Either an invalid code was supplied or the category was deleted or had its code changed by someone else."

  private val categoryCodeFkConstraintFailedCode = "invalid_category_code"

  private def categoryCodePkConstraintFailedMessage(categoryCode: String) =
    s"Category code $categoryCode already exists. Duplicate category codes are not allowed."

  private val categoryCodePkConstraintFailedCode = "duplicate_category_code"

  private def foodNotInCategoryMessage(categoryCode: String, foodCode: String) =
    s"Food $foodCode is not in category $categoryCode, so it cannot be deleted."

  private val foodNotInCategoryCode = "food_not_in_category"

  private def subcategoryNotInCategoryMessage(categoryCode: String, subcategoryCode: String) =
    s"Subcategory $subcategoryCode is not in category $categoryCode, so it cannot be deleted."

  private val subcategoryNotInCategoryCode = "subcategory_not_in_category"

  private def cannotAddCategoryToItselfMessage(categoryCode: String) =
    s"Cannot add $categoryCode to itself."

  private val cannotAddCategoryToItselfCode = "cannot_add_category_to_itself"

  def isCategoryCode(code: String): Either[DatabaseError, Boolean] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT code FROM categories WHERE code={category_code}""").on('category_code -> code).executeQuery().as(SqlParser.str("code").singleOpt).nonEmpty)
  }

  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean] = isCategoryCode(code).right.map(!_)

  def categoryPortionSizeMethods(code: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalCategoryCodeError, Seq[PortionSizeMethod]] = {
    val psmResults =
      SQL("""|WITH v AS(
             | SELECT
             |  (SELECT code FROM categories WHERE code={category_code}) AS category_code,
             |  (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
             |)
             |SELECT v.category_code, v.locale_id, categories_portion_size_methods.id, method, description, image_url, use_for_recipes,
             |categories_portion_size_method_params.id as param_id, name as param_name, value as param_value
             |FROM v 
             |LEFT JOIN categories_portion_size_methods 
             |  ON categories_portion_size_methods.category_code = v.category_code AND categories_portion_size_methods.locale_id = v.locale_id
             |LEFT JOIN categories_portion_size_method_params 
             |  ON categories_portion_size_methods.id = categories_portion_size_method_params.portion_size_method_id
             |ORDER BY param_id""".stripMargin)
        .on('category_code -> code, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndCategoryValidation(psmResults, psmResultRowParser.+)(Seq(FirstRowValidationClause("id", Right(List())))).right.map(mkPortionSizeMethods)
  }

  case class CategoryResultRow(version: UUID, local_version: Option[UUID], code: String, description: String, local_description: Option[String], is_hidden: Boolean, same_as_before_option: Option[Boolean],
    ready_meal_option: Option[Boolean], reasonable_amount: Option[Int])

  def categoryRecord(code: String, locale: String): Either[LocalCategoryCodeError, CategoryRecord] = tryWithConnection {
    implicit conn =>

      val categoryQuery = """|WITH v AS(
                             |  SELECT
                             |    (SELECT code FROM categories WHERE code={category_code}) AS category_code,
                             |    (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
                             |)
                             |SELECT v.category_code, v.locale_id, code, description, local_description, is_hidden, 
                             |  same_as_before_option, ready_meal_option, reasonable_amount, categories.version as version, 
                             |  categories_local.version as local_version 
                             |FROM v
                             |  LEFT JOIN categories ON v.category_code=categories.code
                             |  LEFT JOIN foods_attributes ON v.category_code=categories_attributes.category_code
                             |  LEFT JOIN categories_local ON v.category_code=categories_local.category_code AND v.locale_id=categories_local.locale_id""".stripMargin

      categoryPortionSizeMethods(code, locale).right.flatMap {
        psm =>
          val categoryQueryResult = SQL(categoryQuery).on('category_code -> code, 'locale_id -> locale).executeQuery()

          parseWithLocaleAndCategoryValidation(categoryQueryResult, Macro.namedParser[CategoryResultRow].single)().right.map {
            result =>
              CategoryRecord(
                MainCategoryRecord(result.version, result.code, result.description, result.is_hidden,
                  InheritableAttributes(result.ready_meal_option, result.same_as_before_option, result.reasonable_amount)),
                LocalCategoryRecord(result.local_version, result.local_description, psm))
          }
      }
  }

  def updateCategoryBase(categoryCode: String, categoryBase: MainCategoryRecord): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)

      try {
        SQL("DELETE FROM categories_attributes WHERE category_code={category_code}")
          .on('category_code -> categoryCode).execute()

        SQL("INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})")
          .on('category_code -> categoryCode, 'same_as_before_option -> categoryBase.attributes.sameAsBeforeOption,
            'ready_meal_option -> categoryBase.attributes.readyMealOption, 'reasonable_amount -> categoryBase.attributes.reasonableAmount).execute()

        val rowsAffected = SQL("UPDATE categories SET code = {new_code}, description={description}, is_hidden={is_hidden}, version={new_version}::uuid WHERE code={category_code} AND version={base_version}::uuid")
          .on('category_code -> categoryCode, 'base_version -> categoryBase.version,
            'new_version -> UUID.randomUUID(), 'new_code -> categoryBase.code, 'description -> categoryBase.englishDescription, 'is_hidden -> categoryBase.isHidden)
          .executeUpdate()

        if (rowsAffected == 1) {
          conn.commit()
          Right(())

        } else {
          conn.rollback()
          Left(VersionConflict)
        }

      } catch {
        case e: PSQLException => {
          e.getServerErrorMessage.getConstraint match {
            case "categories_attributes_category_code_fk" => Left(UndefinedCode)
            case _ => throw e
          }
        }
      }
  }

  def updateCategoryLocal(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[LocalUpdateError, Unit] = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)

      try {
        SQL("DELETE FROM categories_portion_size_methods WHERE category_code={category_code} AND locale_id={locale_id}")
          .on('category_code -> categoryCode, 'locale_id -> locale).execute()

        if (categoryLocal.portionSize.nonEmpty) {
          val psmParams = categoryLocal.portionSize.flatMap(ps => Seq[NamedParameter]('category_code -> categoryCode, 'locale_id -> locale, 'method -> ps.method, 'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes))

          val psmKeys = Util.batchKeys(BatchSql("INSERT INTO categories_portion_size_methods VALUES(DEFAULT, {category_code}, {locale_id}, {method}, {description}, {image_url}, {use_for_recipes})", psmParams))

          val psmParamParams = categoryLocal.portionSize.zip(psmKeys).flatMap {
            case (psm, id) => psm.parameters.flatMap(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
          }

          if (psmParamParams.nonEmpty)
            BatchSql("INSERT INTO categories_portion_size_method_params VALUES(DEFAULT, {portion_size_method_id}, {name}, {value})", psmParamParams).execute()
        }

        categoryLocal.version match {
          case Some(version) => {

            // categories_local_category_code_fk
            val rowsAffected = SQL("UPDATE categories_local SET version = {new_version}::uuid, local_description = {local_description} WHERE category_code = {category_code} AND locale_id = {locale_id} AND version = {base_version}::uuid")
              .on('category_code -> categoryCode, 'locale_id -> locale, 'base_version -> categoryLocal.version, 'new_version -> UUID.randomUUID(), 'local_description -> categoryLocal.localDescription)
              .executeUpdate()

            if (rowsAffected == 1) {
              conn.commit()
              Right(())
            } else {
              conn.rollback()
              Left(VersionConflict)
            }
          }
          case None => {
            try {
              SQL("INSERT INTO categories_local VALUES({category_code}, {locale_id}, {local_description}, {version}::uuid)")
                .on('category_code -> categoryCode, 'locale_id -> locale, 'local_description -> categoryLocal.localDescription, 'version -> UUID.randomUUID())
                .execute()
              conn.commit()
              Right(())
            } catch {
              case e: PSQLException =>
                if (e.getServerErrorMessage.getConstraint == "categories_local_pk") {
                  conn.rollback()
                  Left(VersionConflict)
                } else
                  throw e
            }
          }
        }
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "categories_portion_size_methods_categories_code_fk" | "categories_local_category_code_fk" => Left(UndefinedCode)
            case "categories_portion_size_methods_locale_id_fk" | "categories_local_locale_id_fk" => Left(UndefinedLocale)
            case _ => throw e
          }
      }
  }

  def addFoodToCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      try {
        SQL("INSERT INTO foods_categories VALUES(DEFAULT, {food_code},{category_code})")
          .on('category_code -> categoryCode, 'food_code -> foodCode).execute()
        Right(())
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "foods_categories_unique" => Right(())
            case "foods_categories_category_code_fk" => Left(UndefinedCode)
            case "foods_categories_food_code_fk" => Left(UndefinedCode)
          }
      }
  }

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      if (categoryCode == subcategoryCode) {
        Left(DatabaseError(cannotAddCategoryToItselfMessage(categoryCode), None))
      } else
        try {
          SQL("INSERT INTO categories_categories VALUES(DEFAULT, {subcategory_code},{category_code})")
            .on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).execute()
          Right(())
        } catch {
          case e: PSQLException =>
            e.getServerErrorMessage.getConstraint match {
              case "categories_categories_unique" => Right(())
              case "categories_categories_subcategory_code_fk" => Left(UndefinedCode)
              case "categories_categories_category_code_fk" => Left(UndefinedCode)
            }
        }
  }

  def createCategory(categoryBase: NewCategory): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      try {
        SQL("INSERT INTO categories VALUES({code},{description},{is_hidden},{version}::uuid)")
          .on('code -> categoryBase.code, 'description -> categoryBase.englishDescription, 'is_hidden -> categoryBase.isHidden, 'version -> UUID.randomUUID())
          .execute()

        SQL("INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})")
          .on('category_code -> categoryBase.code, 'same_as_before_option -> categoryBase.attributes.sameAsBeforeOption,
            'ready_meal_option -> categoryBase.attributes.readyMealOption, 'reasonable_amount -> categoryBase.attributes.reasonableAmount).execute()

        Right(())
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "categories_pk" => Left(DuplicateCode)
            case "categories_attributes_category_code_fk" => Left(DatabaseError(categoryCodeFkConstraintFailedMessage(categoryBase.code), None))
          }
      }
  }

  def deleteCategory(categoryCode: String): Either[CategoryCodeError, Unit] = tryWithConnection {
    implicit conn =>
      val rowsAffected = SQL("DELETE FROM categories WHERE code={category_code}").on('category_code -> categoryCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        Left(UndefinedCode)
  }

  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      val rowsAffected = SQL("DELETE FROM foods_categories WHERE food_code={food_code} AND category_code={category_code}").on('category_code -> categoryCode, 'food_code -> foodCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        Left(UndefinedCode)
  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>

      val rowsAffected = SQL("DELETE FROM categories_categories WHERE subcategory_code={subcategory_code} AND category_code={category_code}")
        .on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).executeUpdate()

      if (rowsAffected == 1)
        Right(())
      else
        Left(UndefinedCode)
  }
}
