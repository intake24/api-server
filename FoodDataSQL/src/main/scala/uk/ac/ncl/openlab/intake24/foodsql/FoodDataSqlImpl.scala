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

import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.SqlParser.str
import anorm.sqlToSimple
import net.scran24.fooddef.FoodData
import net.scran24.fooddef.PortionSizeMethodParameter
import net.scran24.fooddef.PortionSizeMethod
import net.scran24.fooddef.FoodData
import anorm.Macro
import net.scran24.fooddef.PortionSizeMethod
import java.sql.Connection
import anorm.SqlParser

trait FoodDataSqlImpl extends SqlDataService {

  private case class PsmResultRow(id: Long, method: String, description: String, image_url: String, use_for_recipes: Boolean, param_name: Option[String], param_value: Option[String])

  private case class RecursivePsmResultRow(id: Long, category_code: String, method: String, description: String, image_url: String, use_for_recipes: Boolean, param_name: Option[String], param_value: Option[String])

  private case class RecursiveAttributesRow(same_as_before_option: Option[Boolean], ready_meal_option: Option[Boolean], reasonable_amount: Option[Int])

  private case class FoodRow(code: String, local_description: String, food_group_id: Long)

  private case class NutrientTableRow(nutrient_table_id: String, nutrient_table_code: String)

  private val psmResultRowParser = Macro.namedParser[PsmResultRow]

  private val recursivePsmResultRowParser = Macro.namedParser[RecursivePsmResultRow]

  private val recursiveAttributesRowParser = Macro.namedParser[RecursiveAttributesRow]

  private val foodRowParser = Macro.namedParser[FoodRow]

  val foodPortionSizeMethodsQuery =
    """|SELECT foods_portion_size_methods.id, method, description, image_url, use_for_recipes,
       |foods_portion_size_method_params.id as param_id, name as param_name, value as param_value
       |FROM foods_portion_size_methods LEFT JOIN foods_portion_size_method_params 
       |  ON foods_portion_size_methods.id = foods_portion_size_method_params.portion_size_method_id
       |WHERE food_code = {food_code} AND locale_id = {locale_id} ORDER BY param_id""".stripMargin

  val categoryPortionSizeMethodsQuery =
    """|SELECT categories_portion_size_methods.id, method, description, image_url, use_for_recipes,
       |categories_portion_size_method_params.id as param_id, name as param_name, value as param_value
       |FROM categories_portion_size_methods LEFT JOIN categories_portion_size_method_params 
       |  ON categories_portion_size_methods.id = categories_portion_size_method_params.portion_size_method_id
       |WHERE category_code = {category_code} AND locale_id = {locale_id} ORDER BY param_id""".stripMargin

  private def mkPortionSizeMethods(rows: Seq[PsmResultRow]) =
    // FIXME: surely there is a better method to group records preserving order...
    rows.groupBy(_.id).toSeq.sortBy(_._1).map {
      case (id, rows) =>
        val params = rows.filterNot(r => r.param_name.isEmpty || r.param_value.isEmpty).map(row => PortionSizeMethodParameter(row.param_name.get, row.param_value.get))
        val head = rows.head

        PortionSizeMethod(head.method, head.description, head.image_url, head.use_for_recipes, params)
    }

  private def mkRecursivePortionSizeMethods(rows: Seq[RecursivePsmResultRow]) =
    if (rows.isEmpty)
      Seq()
    else {
      val firstCategoryCode = rows.head.category_code
      mkPortionSizeMethods(rows.takeWhile(_.category_code == firstCategoryCode).map(r => PsmResultRow(r.id, r.method, r.description, r.image_url, r.use_for_recipes, r.param_name, r.param_value)))
    }

  private def resolveLocalPortionSizeMethods(code: String, locale: String)(implicit conn: Connection): Seq[PortionSizeMethod] = {
    val psmResults =
      SQL(foodPortionSizeMethodsQuery).on('food_code -> code, 'locale_id -> locale).executeQuery().as(psmResultRowParser.*)

    if (!psmResults.isEmpty)
      mkPortionSizeMethods(psmResults)
    else {
      // This probably should honour restricted categories list, but it gets messy...
      val inheritedPortionSizesQuery =
        """|WITH RECURSIVE t(code, level) AS (
               |(SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
               |  UNION
               |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
               |)
               |SELECT categories_portion_size_methods.id,
               |       category_code, method, description, image_url, use_for_recipes,
               |       categories_portion_size_method_params.id as param_id, name as param_name, 
               |       value as param_value
               |FROM categories_portion_size_methods
               | JOIN t ON code = category_code
               | LEFT JOIN categories_portion_size_method_params 
               |   ON categories_portion_size_methods.id = categories_portion_size_method_params.portion_size_method_id
               |WHERE categories_portion_size_methods.locale_id = {locale_id}              
               |ORDER BY level, id, param_id""".stripMargin

      mkRecursivePortionSizeMethods(SQL(inheritedPortionSizesQuery).on('food_code -> code, 'locale_id -> locale).executeQuery().as(recursivePsmResultRowParser.*))
    }
  }

  private def localAttributeRows(code: String, locale: String)(implicit conn: Connection) = {
    val attributesQuery =
      """|WITH RECURSIVE t(code, level) AS (
           |(SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
           |   UNION
           |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
           |)
           |(SELECT same_as_before_option, ready_meal_option, reasonable_amount FROM foods_attributes WHERE food_code = {food_code})
           |UNION ALL
           |(SELECT same_as_before_option, ready_meal_option, reasonable_amount FROM categories_attributes JOIN t ON code = category_code ORDER BY level)
           |UNION ALL
           |(SELECT same_as_before_option, ready_meal_option, reasonable_amount FROM attribute_defaults LIMIT 1)
           """.stripMargin

    SQL(attributesQuery).on('food_code -> code).executeQuery().as(recursiveAttributesRowParser.+)
  }

  private def localNutrientTableCodes(code: String, locale: String)(implicit conn: Connection) = {
    SQL("""SELECT nutrient_table_id, nutrient_table_code FROM foods_nutrient_tables WHERE food_code = {food_code} AND locale_id = {locale_id}""")
      .on('food_code -> code, 'locale_id -> locale)
      .as(Macro.namedParser[NutrientTableRow].*).map {
        case NutrientTableRow(id, code) => (id -> code)
      }.toMap
  }

  // Get food data with resolved attribute/portion size method inheritance
  //
  // Portion size methods are resolved in the following order:
  // 1) Local food data
  // 2) Local category data from the nearest parent category that has
  //    portion size methods defined
  // 3) Local food data from the prototype locale
  // 4) Local category data from the nearest parent category
  //    in the prototype locale
  //
  // Category restriction list is currently ignored  
  def foodData(code: String, locale: String): FoodData = tryWithConnection {
    implicit conn =>

      val prototypeLocale = SQL("""SELECT prototype_locale_id FROM locale_prototypes WHERE locale_id = {locale_id}""").on('locale_id -> locale).as(SqlParser.str("prototype_locale_id").singleOpt)

      val portionSizeMethods = {
        val localPsm = resolveLocalPortionSizeMethods(code, locale)

        if (localPsm.isEmpty) {
          prototypeLocale match {
            case Some(prototypeLocale) => {
              resolveLocalPortionSizeMethods(code, prototypeLocale)
            }
            case None => Seq()
          }
        } else
          localPsm
      }

      val attributeRows = localAttributeRows(code, locale) ++ (prototypeLocale match {
        case Some(prototypeLocale) => localAttributeRows(code, prototypeLocale)
        case None => Seq()
      })

      val attributes = attributeRows.tail.foldLeft(attributeRows.head) {
        case (result, row) => {
          RecursiveAttributesRow(
            result.same_as_before_option.orElse(row.same_as_before_option),
            result.ready_meal_option.orElse(row.ready_meal_option),
            result.reasonable_amount.orElse(row.reasonable_amount))
        }
      }
      
      val nutrientTableCodes = {
        val localCodes = localNutrientTableCodes(code, locale)
        
        if (localCodes.isEmpty) prototypeLocale match {
          case Some(prototypeLocale) => localNutrientTableCodes(code, prototypeLocale)
          case None => Map[String, String]()
        } else
          localCodes
      }

      val foodQuery =
        """|SELECT code, COALESCE(t1.local_description, t2.local_description) AS local_description, food_group_id
           |FROM foods
           |  LEFT JOIN foods_local as t1 ON foods.code = t1.food_code AND t1.locale_id = {locale_id}
           |  LEFT JOIN foods_local as t2 ON foods.code = t2.food_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locale_prototypes WHERE locale_id = {locale_id})
           |WHERE code = {food_code} AND (t1.local_description IS NOT NULL OR t2.local_description IS NOT NULL)""".stripMargin

      val foodRow = SQL(foodQuery).on('food_code -> code, 'locale_id -> locale).executeQuery().as(foodRowParser.single)

      FoodData(foodRow.code, foodRow.local_description, nutrientTableCodes, foodRow.food_group_id.toInt, portionSizeMethods,
        attributes.ready_meal_option.get, attributes.same_as_before_option.get, attributes.reasonable_amount.get)
  }
}
