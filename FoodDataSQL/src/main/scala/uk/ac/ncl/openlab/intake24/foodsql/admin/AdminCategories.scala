package uk.ac.ncl.openlab.intake24.foodsql.admin

import java.util.UUID
import uk.ac.ncl.openlab.intake24.services.CodeError
import uk.ac.ncl.openlab.intake24.CategoryRecord
import anorm._
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.services.VersionConflict
import uk.ac.ncl.openlab.intake24.services.SqlException
import uk.ac.ncl.openlab.intake24.services.InvalidRequest
import uk.ac.ncl.openlab.intake24.services.NewCategory
import org.postgresql.util.PSQLException

import anorm.NamedParameter.symbol
import scala.Left
import scala.Right

import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.foodsql.Util

import org.postgresql.util.PSQLException

trait AdminCategories extends SqlDataService with AdminPortionSizeShared with AdminErrorMessagesShared {

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
  
    def isCategoryCode(code: String): Boolean = tryWithConnection {
    implicit conn =>
      SQL("""SELECT code FROM categories WHERE code={category_code}""").on('category_code -> code).executeQuery().as(SqlParser.str("code").*).nonEmpty
  }


  def isCategoryCodeAvailable(code: String) = tryWithConnection {
    implicit conn =>
      SQL("SELECT code FROM categories WHERE code={category_code}").on('category_code -> code).executeQuery().as(SqlParser.str("code").*).isEmpty
  }

  private val categoryPortionSizeMethodsQuery =
    """|SELECT categories_portion_size_methods.id, method, description, image_url, use_for_recipes,
       |categories_portion_size_method_params.id as param_id, name as param_name, value as param_value
       |FROM categories_portion_size_methods LEFT JOIN categories_portion_size_method_params 
       |  ON categories_portion_size_methods.id = categories_portion_size_method_params.portion_size_method_id
       |WHERE category_code = {category_code} AND locale_id = {locale_id} ORDER BY param_id""".stripMargin

  case class CategoryResultRow(version: UUID, local_version: Option[UUID], code: String, description: String, local_description: Option[String], is_hidden: Boolean, same_as_before_option: Option[Boolean],
    ready_meal_option: Option[Boolean], reasonable_amount: Option[Int])

  def categoryRecord(code: String, locale: String): Either[CodeError, CategoryRecord] = tryWithConnection {
    implicit conn =>
      val psmResults =
        SQL(categoryPortionSizeMethodsQuery).on('category_code -> code, 'locale_id -> locale).executeQuery().as(psmResultRowParser.*)

      val portionSizeMethods = mkPortionSizeMethods(psmResults)

      val categoryQuery =
        """|SELECT categories.version as version, categories_local.version as local_version, code, description, local_description, 
           |       is_hidden, same_as_before_option, ready_meal_option, reasonable_amount 
           |FROM categories 
           |     INNER JOIN categories_attributes ON categories.code = categories_attributes.category_code
           |     LEFT JOIN categories_local ON categories.code = categories_local.category_code AND categories_local.locale_id = {locale_id} 
           |WHERE code = {category_code}""".stripMargin

      val categoryRowParser = Macro.namedParser[CategoryResultRow]

      SQL(categoryQuery).on('category_code -> code, 'locale_id -> locale).executeQuery().as(categoryRowParser.singleOpt) match {
        case Some(record) => {
          Right(CategoryRecord(
            MainCategoryRecord(record.version, record.code, record.description, record.is_hidden,
              InheritableAttributes(record.ready_meal_option, record.same_as_before_option, record.reasonable_amount)),
            LocalCategoryRecord(record.local_version, record.local_description, portionSizeMethods)))
        }
        case None => Left(CodeError.UndefinedCode)
      }
  }

  def updateCategoryBase(categoryCode: String, categoryBase: MainCategoryRecord) = tryWithConnection {
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
          uk.ac.ncl.openlab.intake24.services.Success

        } else {
          conn.rollback()
          VersionConflict
        }

      } catch {
        case e: PSQLException => {
          conn.rollback()
          e.getServerErrorMessage.getConstraint match {
            case "categories_attributes_category_code_fk" => InvalidRequest(categoryCodeFkConstraintFailedCode, categoryCodeFkConstraintFailedMessage(categoryCode))
            case _ => SqlException(e.getServerErrorMessage.getMessage)
          }
        }
      }
  }

  def updateCategoryLocal(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord) = tryWithConnection {
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
              uk.ac.ncl.openlab.intake24.services.Success
            } else {
              conn.rollback()
              VersionConflict
            }
          }
          case None => {
            try {
              SQL("INSERT INTO categories_local VALUES({category_code}, {locale_id}, {local_description}, {version}::uuid)")
                .on('category_code -> categoryCode, 'locale_id -> locale, 'local_description -> categoryLocal.localDescription, 'version -> UUID.randomUUID())
                .execute()
              conn.commit()
              uk.ac.ncl.openlab.intake24.services.Success
            } catch {
              case e: PSQLException =>
                if (e.getServerErrorMessage.getConstraint == "categories_local_pk") {
                  conn.rollback()
                  VersionConflict
                } else
                  throw e
            }
          }
        }

      } catch {
        case e: PSQLException => {
          conn.rollback()
          e.getServerErrorMessage.getConstraint match {
            case "categories_portion_size_methods_categories_code_fk" | "categories_local_category_code_fk" => InvalidRequest(categoryCodeFkConstraintFailedCode, categoryCodeFkConstraintFailedMessage(categoryCode))
            case "categories_portion_size_methods_locale_id_fk" | "categories_local_locale_id_fk" => InvalidRequest(localeFkConstraintFailedCode, localeFkConstraintFailedMessage(locale))
            case _ => SqlException(e.getServerErrorMessage.getMessage)
          }
        }
      }
  }

  def addFoodToCategory(categoryCode: String, foodCode: String) = tryWithConnection {
    implicit conn =>
      try {
        SQL("INSERT INTO foods_categories VALUES(DEFAULT, {food_code},{category_code})")
          .on('category_code -> categoryCode, 'food_code -> foodCode).execute()
        uk.ac.ncl.openlab.intake24.services.Success
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "foods_categories_unique" => uk.ac.ncl.openlab.intake24.services.Success
            case "foods_categories_category_code_fk" => InvalidRequest(categoryCodeFkConstraintFailedCode, categoryCodeFkConstraintFailedMessage(categoryCode))
            case "foods_categories_food_code_fk" => InvalidRequest(foodCodeFkConstraintFailedCode, foodCodeFkConstraintFailedMessage(foodCode))
            case _ => SqlException(e.getServerErrorMessage.getMessage)
          }
      }
  }

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String) = tryWithConnection {
    implicit conn =>
      if (categoryCode == subcategoryCode) {
        InvalidRequest(cannotAddCategoryToItselfCode, cannotAddCategoryToItselfMessage(categoryCode))
      } else
        try {
          SQL("INSERT INTO categories_categories VALUES(DEFAULT, {subcategory_code},{category_code})")
            .on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).execute()
          uk.ac.ncl.openlab.intake24.services.Success
        } catch {
          case e: PSQLException =>
            e.getServerErrorMessage.getConstraint match {
              case "categories_categories_unique" => uk.ac.ncl.openlab.intake24.services.Success
              case "categories_categories_subcategory_code_fk" => InvalidRequest(categoryCodeFkConstraintFailedCode, categoryCodeFkConstraintFailedMessage(subcategoryCode))
              case "categories_categories_category_code_fk" => InvalidRequest(categoryCodeFkConstraintFailedCode, categoryCodeFkConstraintFailedMessage(categoryCode))

              case _ => SqlException(e.getServerErrorMessage.getMessage)
            }
        }
  }

  def createCategory(categoryBase: NewCategory) = tryWithConnection {
    implicit conn =>
      try {
        SQL("INSERT INTO categories VALUES({code},{description},{is_hidden},{version}::uuid)")
          .on('code -> categoryBase.code, 'description -> categoryBase.englishDescription, 'is_hidden -> categoryBase.isHidden, 'version -> UUID.randomUUID())
          .execute()

        SQL("INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})")
          .on('category_code -> categoryBase.code, 'same_as_before_option -> categoryBase.attributes.sameAsBeforeOption,
            'ready_meal_option -> categoryBase.attributes.readyMealOption, 'reasonable_amount -> categoryBase.attributes.reasonableAmount).execute()

        uk.ac.ncl.openlab.intake24.services.Success
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "categories_pk" => InvalidRequest(categoryCodePkConstraintFailedCode, categoryCodePkConstraintFailedMessage(categoryBase.code))
            case "categories_attributes_category_code_fk" => InvalidRequest(categoryCodeFkConstraintFailedCode, categoryCodeFkConstraintFailedMessage(categoryBase.code))
            case _ => SqlException(e.getServerErrorMessage.getMessage)
          }
      }
  }

  def deleteCategory(categoryCode: String) = tryWithConnection {
    implicit conn =>
      try {
        val rowsAffected = SQL("DELETE FROM categories WHERE code={category_code}").on('category_code -> categoryCode).executeUpdate()

        if (rowsAffected == 1)
          uk.ac.ncl.openlab.intake24.services.Success
        else
          InvalidRequest(categoryCodeFkConstraintFailedCode, categoryCodeFkConstraintFailedMessage(categoryCode))
      } catch {
        case e: PSQLException => SqlException(e.getServerErrorMessage.getMessage)
      }
  }

  def removeFoodFromCategory(categoryCode: String, foodCode: String) = tryWithConnection {
    implicit conn =>
      try {
        val rowsAffected = SQL("DELETE FROM foods_categories WHERE food_code={food_code} AND category_code={category_code}").on('category_code -> categoryCode, 'food_code -> foodCode).executeUpdate()

        if (rowsAffected == 1)
          uk.ac.ncl.openlab.intake24.services.Success
        else
          InvalidRequest(foodNotInCategoryCode, foodNotInCategoryMessage(categoryCode, foodCode))
      } catch {
        case e: PSQLException => SqlException(e.getServerErrorMessage.getMessage)
      }

  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String) = tryWithConnection {
    implicit conn =>
      try {
        val rowsAffected = SQL("DELETE FROM categories_categories WHERE subcategory_code={subcategory_code} AND category_code={category_code}")
          .on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).executeUpdate()

        if (rowsAffected == 1)
          uk.ac.ncl.openlab.intake24.services.Success
        else
          InvalidRequest(subcategoryNotInCategoryCode, subcategoryNotInCategoryMessage(categoryCode, subcategoryCode))
      } catch {
        case e: PSQLException => SqlException(e.getServerErrorMessage.getMessage)
      }
  }
}
