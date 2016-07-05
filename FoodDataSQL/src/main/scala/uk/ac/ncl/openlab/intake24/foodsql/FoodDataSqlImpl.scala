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

import scala.Left
import scala.Right

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.UserFoodData
import uk.ac.ncl.openlab.intake24.services.FoodDataError
import uk.ac.ncl.openlab.intake24.services.FoodDataSources
import uk.ac.ncl.openlab.intake24.services.InheritableAttributeSource
import uk.ac.ncl.openlab.intake24.services.InheritableAttributeSources
import uk.ac.ncl.openlab.intake24.services.SourceLocale
import uk.ac.ncl.openlab.intake24.services.SourceRecord

trait FoodDataSqlImpl extends SqlDataService {

  val fallbackLocale = "en_GB"

  private case class PsmResultRow(id: Long, method: String, description: String, image_url: String, use_for_recipes: Boolean, param_name: Option[String], param_value: Option[String])

  private case class RecursivePsmResultRow(id: Long, category_code: String, method: String, description: String, image_url: String, use_for_recipes: Boolean, param_name: Option[String], param_value: Option[String])

  private case class FoodRow(code: String, local_description: Option[String], prototype_description: Option[String], food_group_id: Long)

  private case class NutrientTableRow(nutrient_table_id: String, nutrient_table_code: String)

  private val psmResultRowParser = Macro.namedParser[PsmResultRow]

  private val recursivePsmResultRowParser = Macro.namedParser[RecursivePsmResultRow]

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

  private def mkRecursivePortionSizeMethods(rows: Seq[RecursivePsmResultRow]): (Seq[PortionSizeMethod], String) =
    if (rows.isEmpty)
      (Seq(), "")
    else {
      val firstCategoryCode = rows.head.category_code
      (mkPortionSizeMethods(rows.takeWhile(_.category_code == firstCategoryCode).map(r => PsmResultRow(r.id, r.method, r.description, r.image_url, r.use_for_recipes, r.param_name, r.param_value))), firstCategoryCode)
    }

  private def resolveLocalPortionSizeMethods(code: String, locale: String)(implicit conn: Connection): (Seq[PortionSizeMethod], SourceRecord) = {
    val psmResults =
      SQL(foodPortionSizeMethodsQuery).on('food_code -> code, 'locale_id -> locale).executeQuery().as(psmResultRowParser.*)

    if (!psmResults.isEmpty)
      (mkPortionSizeMethods(psmResults), SourceRecord.FoodRecord(code))
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

      val (methods, category) = mkRecursivePortionSizeMethods(SQL(inheritedPortionSizesQuery).on('food_code -> code, 'locale_id -> locale).executeQuery().as(recursivePsmResultRowParser.*))

      (methods, SourceRecord.CategoryRecord(category))
    }
  }

  private case class DefaultAttributesRow(same_as_before_option: Boolean, ready_meal_option: Boolean, reasonable_amount: Int)

  private def defaultAttributesRow(implicit conn: Connection) = {
    SQL("""""").executeQuery().as(Macro.namedParser[DefaultAttributesRow].single)
  }

  private case class RecursiveAttributesRow(same_as_before_option: Option[Boolean], ready_meal_option: Option[Boolean], reasonable_amount: Option[Int], is_food_record: Boolean, code: Option[String])

  private def attributeRows(code: String)(implicit conn: Connection) = {
    val attributesQuery =
      """|WITH RECURSIVE t(code, level) AS (
           |(SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
           |   UNION
           |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
           |)
           |(SELECT same_as_before_option, ready_meal_option, reasonable_amount, true as is_food_record, food_code as code FROM foods_attributes WHERE food_code = {food_code})
           |UNION ALL
           |(SELECT same_as_before_option, ready_meal_option, reasonable_amount, false as is_food_record, category_code as code FROM categories_attributes JOIN t ON code = category_code ORDER BY level)
           |UNION ALL
           |(SELECT same_as_before_option, ready_meal_option, reasonable_amount, false as is_food_record, NULL as code FROM attribute_defaults LIMIT 1)
           """.stripMargin

    SQL(attributesQuery).on('food_code -> code).executeQuery().as(Macro.namedParser[RecursiveAttributesRow].+)
  }

  private def localNutrientTableCodes(code: String, locale: String)(implicit conn: Connection) = {
    SQL("""SELECT nutrient_table_id, nutrient_table_code FROM foods_nutrient_tables WHERE food_code = {food_code} AND locale_id = {locale_id}""")
      .on('food_code -> code, 'locale_id -> locale)
      .as(Macro.namedParser[NutrientTableRow].*).map {
        case NutrientTableRow(id, code) => (id -> code)
      }.toMap
  }

  def foodAllCategories(code: String): Seq[String] = tryWithConnection {
    implicit conn =>
      val query = """|WITH RECURSIVE t(code, level) AS (
                   |(SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
                   | UNION ALL
                   |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
                   |)
                   |SELECT code
                   |FROM t 
                   |ORDER BY level""".stripMargin
      SQL(query).on('food_code -> code).executeQuery().as(SqlParser.str("code").*)
  }
  
  def categoryAllCategories(code: String): Seq[String] = tryWithConnection {
    implicit conn =>
      val query = """|WITH RECURSIVE t(code, level) AS (
                   |(SELECT category_code as code, 0 as level FROM categories_categories WHERE subcategory_code = {category_code} ORDER BY code)
                   | UNION ALL
                   |(SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
                   |)
                   |SELECT code
                   |FROM t 
                   |ORDER BY level""".stripMargin
      SQL(query).on('category_code -> code).executeQuery().as(SqlParser.str("code").*)
  }
  
  def isCategoryCode(code: String): Boolean = tryWithConnection {
    implicit conn =>
      val query = """SELECT COUNT(*) FROM categories WHERE code={category_code}"""
      (SQL(query).on('category_code -> code).executeQuery().as(SqlParser.long("count").single) == 1)      
  }

  private case class InheritableAttributesFinal(sameAsBeforeOption: Boolean, readyMealOption: Boolean, reasonableAmount: Int, sources: InheritableAttributeSources)

  private case class InheritableAttributeTemp(
      sameAsBeforeOption: Option[(Boolean, InheritableAttributeSource)],
      readyMealOption: Option[(Boolean, InheritableAttributeSource)],
      reasonableAmount: Option[(Int, InheritableAttributeSource)]) {
    def finalise = InheritableAttributesFinal(sameAsBeforeOption.get._1, readyMealOption.get._1, reasonableAmount.get._1,
      InheritableAttributeSources(sameAsBeforeOption.get._2, readyMealOption.get._2, reasonableAmount.get._2))
  }

  def inheritableAttributeSource(row: RecursiveAttributesRow): InheritableAttributeSource = row.code match {
    case Some(code) =>
      if (row.is_food_record)
        InheritableAttributeSource.FoodRecord(code)
      else
        InheritableAttributeSource.CategoryRecord(code)
    case None => InheritableAttributeSource.Default
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
  def foodData(code: String, locale: String): Either[FoodDataError, (UserFoodData, FoodDataSources)] = tryWithConnection {
    implicit conn =>

      val prototypeLocale = SQL("""SELECT prototype_locale_id FROM locales WHERE id = {locale_id}""").on('locale_id -> locale).executeQuery().as(SqlParser.str("prototype_locale_id").?.single)

      val (portionSizeMethods, portionSizeMethodsSource) = {
        val (localPsm, localPsmSrcRec) = resolveLocalPortionSizeMethods(code, locale)

        if (localPsm.isEmpty) {
          prototypeLocale.map(resolveLocalPortionSizeMethods(code, _)) match {
            case Some((protoPsm, protoPsmSrcRec)) if protoPsm.nonEmpty => (protoPsm, (SourceLocale.Prototype(prototypeLocale.get), protoPsmSrcRec))
            case _ => {
              val (fallbackPsm, fallbackPsmSrcRec) = resolveLocalPortionSizeMethods(code, fallbackLocale)
              (fallbackPsm, (SourceLocale.Fallback(fallbackLocale), fallbackPsmSrcRec))
            }
          }
        } else
          (localPsm, (SourceLocale.Current(locale), localPsmSrcRec))
      }

      val attrRows = attributeRows(code)

      val attrFirstRowSrc = inheritableAttributeSource(attrRows.head)

      val attrTemp = InheritableAttributeTemp(
        attrRows.head.same_as_before_option.map((_, attrFirstRowSrc)),
        attrRows.head.ready_meal_option.map((_, attrFirstRowSrc)),
        attrRows.head.reasonable_amount.map((_, attrFirstRowSrc)))

      val attributes = attrRows.tail.foldLeft(attrTemp) {
        case (result, row) => {

          val rowSrc = inheritableAttributeSource(row)

          InheritableAttributeTemp(
            result.sameAsBeforeOption.orElse(row.same_as_before_option.map((_, rowSrc))),
            result.readyMealOption.orElse(row.ready_meal_option.map((_, rowSrc))),
            result.reasonableAmount.orElse(row.reasonable_amount.map((_, rowSrc))))
        }
      }.finalise

      val (nutrientTableCodes, nutrientTableCodesSrc) = {
        val localCodes = localNutrientTableCodes(code, locale)

        if (localCodes.isEmpty) prototypeLocale.map(pl => (pl, localNutrientTableCodes(code, pl))) match {
          case Some((ploc, prototypeCodes)) if prototypeCodes.nonEmpty => (prototypeCodes, SourceLocale.Prototype(ploc))
          case _ => (localNutrientTableCodes(code, fallbackLocale), SourceLocale.Fallback(fallbackLocale))
        }
        else
          (localCodes, SourceLocale.Current(locale))
      }

      val foodQuery =
        """|SELECT code, t1.local_description as local_description, t2.local_description AS prototype_description, food_group_id
           |FROM foods
           |  LEFT JOIN foods_local as t1 ON foods.code = t1.food_code AND t1.locale_id = {locale_id}
           |  LEFT JOIN foods_local as t2 ON foods.code = t2.food_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id})
           |WHERE code = {food_code}""".stripMargin

      val foodRow = SQL(foodQuery).on('food_code -> code, 'locale_id -> locale).executeQuery().as(foodRowParser.singleOpt)

      foodRow match {
        case Some(row) => (row.local_description, row.prototype_description) match {
          case (Some(localDescription), _) => {
            val data = UserFoodData(row.code, localDescription, nutrientTableCodes, row.food_group_id.toInt, portionSizeMethods,
              attributes.readyMealOption, attributes.sameAsBeforeOption, attributes.reasonableAmount)
            val sources = FoodDataSources(SourceLocale.Current(locale), nutrientTableCodesSrc, portionSizeMethodsSource, attributes.sources)
            Right((data, sources))
          }
          case (None, Some(prototypeDescription)) => {
            val data = UserFoodData(row.code, prototypeDescription, nutrientTableCodes, row.food_group_id.toInt, portionSizeMethods,
              attributes.readyMealOption, attributes.sameAsBeforeOption, attributes.reasonableAmount)
            val sources = FoodDataSources(SourceLocale.Prototype(locale), nutrientTableCodesSrc, portionSizeMethodsSource, attributes.sources)
            Right((data, sources))
          }
          case (None, None) => Left(FoodDataError.NoLocalDescription)
        }
        case None => Left(FoodDataError.UndefinedCode)
      }
  }
}
