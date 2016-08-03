/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.ncl.openlab.intake24.foodsql

import java.sql.Connection
import java.util.UUID

import org.postgresql.util.PSQLException

import com.google.inject.Singleton

import anorm._

import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.services.InvalidRequest
import uk.ac.ncl.openlab.intake24.services.NewCategory
import uk.ac.ncl.openlab.intake24.services.NewFood
import uk.ac.ncl.openlab.intake24.services.SqlException
import uk.ac.ncl.openlab.intake24.services.UpdateResult
import uk.ac.ncl.openlab.intake24.services.VersionConflict
import uk.ac.ncl.openlab.intake24.AssociatedFood

@Singleton
trait FoodDataEditingSqlImpl extends SqlDataService {

  def foodCodeFkConstraintFailedMessage(foodCode: String) =
    s"Food code $foodCode is not defined. Either an invalid code was supplied or the food was deleted or had its code changed by someone else."

  val foodCodeFkConstraintFailedCode = "invalid_food_code"

  def foodCodePkConstraintFailedMessage(foodCode: String) =
    s"Food code $foodCode already exists. Duplicate food codes are not allowed."

  val foodCodePkConstraintFailedCode = "duplicate_food_code"

  def categoryCodeFkConstraintFailedMessage(categoryCode: String) =
    s"Category code $categoryCode is not defined. Either an invalid code was supplied or the category was deleted or had its code changed by someone else."

  val categoryCodeFkConstraintFailedCode = "invalid_category_code"

  def categoryCodePkConstraintFailedMessage(categoryCode: String) =
    s"Category code $categoryCode already exists. Duplicate category codes are not allowed."

  val categoryCodePkConstraintFailedCode = "duplicate_category_code"

  def localeFkConstraintFailedMessage(locale: String) =
    s"Locale $locale is not defined."

  val localeFkConstraintFailedCode = "invalid_locale_code"

  def foodNotInCategoryMessage(categoryCode: String, foodCode: String) =
    s"Food $foodCode is not in category $categoryCode, so it cannot be deleted."

  val foodNotInCategoryCode = "food_not_in_category"

  def subcategoryNotInCategoryMessage(categoryCode: String, subcategoryCode: String) =
    s"Subcategory $subcategoryCode is not in category $categoryCode, so it cannot be deleted."

  val subcategoryNotInCategoryCode = "subcategory_not_in_category"

  def cannotAddCategoryToItselfMessage(categoryCode: String) =
    s"Cannot add $categoryCode to itself."

  val cannotAddCategoryToItselfCode = "cannot_add_category_to_itself"

  val temporaryCodesExhausted = "temporary_codes_exhausted"

  val temporaryCodesExhaustedMessage = "Cannot assign a temporary food code, tried F000 through F999 but none are available."

  def isFoodCodeAvailable(code: String) = tryWithConnection {
    implicit conn =>
      SQL("SELECT code FROM foods WHERE code={food_code}").on('food_code -> code).executeQuery().as(SqlParser.str("code").*).isEmpty
  }

  def createFood(newFood: NewFood) = tryWithConnection {
    implicit conn =>
      try {
        SQL(Queries.foodsInsert)
          .on('code -> newFood.code, 'description -> newFood.englishDescription, 'food_group_id -> newFood.groupCode, 'version -> UUID.randomUUID())
          .execute()

        SQL(Queries.foodsAttributesInsert).on('food_code -> newFood.code, 'same_as_before_option -> newFood.attributes.sameAsBeforeOption,
          'ready_meal_option -> newFood.attributes.readyMealOption, 'reasonable_amount -> newFood.attributes.reasonableAmount).execute()

        uk.ac.ncl.openlab.intake24.services.Success
      } catch {
        case e: PSQLException =>
          e.getServerErrorMessage.getConstraint match {
            case "foods_code_pk" => InvalidRequest(foodCodePkConstraintFailedCode, foodCodePkConstraintFailedMessage(newFood.code))
            case "foods_attributes_food_code_fk" => InvalidRequest(foodCodeFkConstraintFailedCode, foodCodeFkConstraintFailedMessage(newFood.code))
            case _ => SqlException(e.getServerErrorMessage.getMessage)
          }
      }
  }

  def createFoodWithTempCode(newFood: NewFood): Either[InvalidRequest, String] = {
    def tryNextNumber(n: Int): Either[InvalidRequest, String] = {
      if (n > 999)
        Left(InvalidRequest(temporaryCodesExhausted, temporaryCodesExhaustedMessage))
      else {
        val tempCode = "F%03d".format(n)
        createFood(newFood.copy(code = tempCode)) match {
          case InvalidRequest(errorCode, _) if errorCode == foodCodePkConstraintFailedCode => tryNextNumber(n + 1)
          case x: InvalidRequest => Left(x)
          case uk.ac.ncl.openlab.intake24.services.Success => Right(tempCode)
        }
      }
    }

    tryNextNumber(0)
  }

  def deleteFood(foodCode: String) = tryWithConnection {
    implicit conn =>
      try {
        val rowsAffected = SQL(Queries.foodsDelete).on('food_code -> foodCode).executeUpdate()

        if (rowsAffected == 1)
          uk.ac.ncl.openlab.intake24.services.Success
        else
          InvalidRequest(foodCodeFkConstraintFailedCode, foodCodeFkConstraintFailedMessage(foodCode))
      } catch {
        case e: PSQLException => SqlException(e.getServerErrorMessage.getMessage)
      }
  }

  def updateFoodBase(foodCode: String, foodBase: MainFoodRecord) = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)

      try {
        SQL(Queries.foodsAttributesDelete).on('food_code -> foodCode).execute()

        SQL(Queries.foodsAttributesInsert).on('food_code -> foodCode, 'same_as_before_option -> foodBase.attributes.sameAsBeforeOption,
          'ready_meal_option -> foodBase.attributes.readyMealOption, 'reasonable_amount -> foodBase.attributes.reasonableAmount).execute()

        val rowsAffected = SQL(Queries.foodsUpdate)
          .on('food_code -> foodCode, 'base_version -> foodBase.version,
            'new_version -> UUID.randomUUID(), 'new_code -> foodBase.code, 'description -> foodBase.englishDescription, 'food_group_id -> foodBase.groupCode)
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
            case "foods_attributes_food_code_fk" => InvalidRequest(foodCodeFkConstraintFailedCode, foodCodeFkConstraintFailedMessage(foodCode))
            case _ => SqlException(e.getServerErrorMessage.getMessage)
          }
        }
      }
  }

  def updateFoodLocal(foodCode: String, locale: String, foodLocal: LocalFoodRecord) = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)

      try {
        SQL(Queries.foodNutrientTablesDelete).on('food_code -> foodCode, 'locale_id -> locale).execute()

        SQL(Queries.foodsPortionSizeMethodsDelete).on('food_code -> foodCode, 'locale_id -> locale).execute()

        if (foodLocal.nutrientTableCodes.nonEmpty) {
          val nutrientTableCodesParams = foodLocal.nutrientTableCodes.map {
            case (table_id, table_code) => Seq[NamedParameter]('food_code -> foodCode, 'locale_id -> locale, 'nutrient_table_id -> table_id, 'nutrient_table_code -> table_code)
          }.toSeq

          BatchSql(Queries.foodNutrientTablesInsert, nutrientTableCodesParams).execute()
        }

        if (foodLocal.portionSize.nonEmpty) {
          val psmParams = foodLocal.portionSize.map(ps => Seq[NamedParameter]('food_code -> foodCode, 'locale_id -> locale, 'method -> ps.method, 'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes))

          val psmKeys = Util.batchKeys(BatchSql(Queries.foodsPortionSizeMethodsInsert, psmParams))

          val psmParamParams = foodLocal.portionSize.zip(psmKeys).flatMap {
            case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
          }

          if (psmParamParams.nonEmpty)
            BatchSql(Queries.foodsPortionSizeMethodsParamsInsert, psmParamParams).execute()
        }

        foodLocal.version match {
          case Some(version) => {

            val rowsAffected = SQL(Queries.foodsLocalUpdate)
              .on('food_code -> foodCode, 'locale_id -> locale, 'base_version -> foodLocal.version, 'new_version -> UUID.randomUUID(), 'local_description -> foodLocal.localDescription, 'do_not_use -> foodLocal.doNotUse)
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
              SQL(Queries.foodsLocalInsert)
                .on('food_code -> foodCode, 'locale_id -> locale, 'local_description -> foodLocal.localDescription, 'do_not_use -> foodLocal.doNotUse, 'version -> UUID.randomUUID())
                .execute()
              conn.commit()
              uk.ac.ncl.openlab.intake24.services.Success
            } catch {
              case e: PSQLException =>
                if (e.getServerErrorMessage.getConstraint == "foods_local_pk") {
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
            case "foods_nutrient_tables_food_code_fk" | "foods_portion_size_methods_food_id_fk" | "foods_local_food_code_fk" => InvalidRequest(foodCodeFkConstraintFailedCode, foodCodeFkConstraintFailedMessage(foodCode))
            case "foods_nutrient_tables_locale_id_fk" | "foods_portion_size_methods_locale_id_fk" | "food_local_locale_id_fk" => InvalidRequest(localeFkConstraintFailedCode, localeFkConstraintFailedMessage(locale))
            case _ => SqlException(e.getServerErrorMessage.getMessage)
          }
        }
      }
  }

  def updateCategoryBase(categoryCode: String, categoryBase: MainCategoryRecord) = tryWithConnection {
    implicit conn =>
      conn.setAutoCommit(false)

      try {
        SQL(Queries.categoriesAttributesDelete).on('category_code -> categoryCode).execute()

        SQL(Queries.categoriesAttributesInsert).on('category_code -> categoryCode, 'same_as_before_option -> categoryBase.attributes.sameAsBeforeOption,
          'ready_meal_option -> categoryBase.attributes.readyMealOption, 'reasonable_amount -> categoryBase.attributes.reasonableAmount).execute()

        val rowsAffected = SQL(Queries.categoriesUpdate)
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
        SQL(Queries.categoriesPortionSizeMethodsDelete).on('category_code -> categoryCode, 'locale_id -> locale).execute()

        if (categoryLocal.portionSize.nonEmpty) {
          val psmParams = categoryLocal.portionSize.map(ps => Seq[NamedParameter]('category_code -> categoryCode, 'locale_id -> locale, 'method -> ps.method, 'description -> ps.description, 'image_url -> ps.imageUrl, 'use_for_recipes -> ps.useForRecipes))

          val psmKeys = Util.batchKeys(BatchSql(Queries.categoriesPortionSizeMethodsInsert, psmParams))

          val psmParamParams = categoryLocal.portionSize.zip(psmKeys).flatMap {
            case (psm, id) => psm.parameters.map(param => Seq[NamedParameter]('portion_size_method_id -> id, 'name -> param.name, 'value -> param.value))
          }

          if (psmParamParams.nonEmpty)
            BatchSql(Queries.categoriesPortionSizeMethodParamsInsert, psmParamParams).execute()
        }

        categoryLocal.version match {
          case Some(version) => {

            // categories_local_category_code_fk
            val rowsAffected = SQL(Queries.categoriesLocalUpdate)
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
              SQL(Queries.categoriesLocalInsert)
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
        SQL(Queries.foodsCategoriesInsert).on('category_code -> categoryCode, 'food_code -> foodCode).execute()
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
          SQL(Queries.categoriesCategoriesInsert).on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).execute()
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

  def isCategoryCodeAvailable(code: String) = tryWithConnection {
    implicit conn =>
      SQL("SELECT code FROM categories WHERE code={category_code}").on('category_code -> code).executeQuery().as(SqlParser.str("code").*).isEmpty
  }

  def createCategory(categoryBase: NewCategory) = tryWithConnection {
    implicit conn =>
      try {
        SQL(Queries.categoriesInsert)
          .on('code -> categoryBase.code, 'description -> categoryBase.englishDescription, 'is_hidden -> categoryBase.isHidden, 'version -> UUID.randomUUID())
          .execute()

        SQL(Queries.categoriesAttributesInsert).on('category_code -> categoryBase.code, 'same_as_before_option -> categoryBase.attributes.sameAsBeforeOption,
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
        val rowsAffected = SQL(Queries.categoriesDelete).on('category_code -> categoryCode).executeUpdate()

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
        val rowsAffected = SQL(Queries.foodsCategoriesDelete).on('category_code -> categoryCode, 'food_code -> foodCode).executeUpdate()

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
        val rowsAffected = SQL(Queries.categoriesCategoriesDelete).on('category_code -> categoryCode, 'subcategory_code -> subcategoryCode).executeUpdate()

        if (rowsAffected == 1)
          uk.ac.ncl.openlab.intake24.services.Success
        else
          InvalidRequest(subcategoryNotInCategoryCode, subcategoryNotInCategoryMessage(categoryCode, subcategoryCode))
      } catch {
        case e: PSQLException => SqlException(e.getServerErrorMessage.getMessage)
      }
  }
}
