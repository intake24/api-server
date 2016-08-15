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

package uk.ac.ncl.openlab.intake24.foodsql.user

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
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.NoLocalDescription
import uk.ac.ncl.openlab.intake24.services.fooddb.user.InheritableAttributeSources
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodDataError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedCode
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalFoodCodeError
import uk.ac.ncl.openlab.intake24.foodsql.shared.FoodPortionSizeShared
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDataService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.SourceRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.user.SourceLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDataSources

trait FoodDataUserImpl extends FoodDataService
    with SqlDataService
    with SqlResourceLoader
    with FirstRowValidation
    with InheritedAttributesImpl
    with InheritedPortionSizeMethodsImpl
    with InheritedNutrientTableCodesImpl {

  private case class FoodRow(code: String, english_description: String, local_description: Option[String], prototype_description: Option[String], food_group_id: Long)

  private def prototypeLocale(locale: String)(implicit conn: java.sql.Connection): Either[LocaleError, Option[String]] = {
    SQL("""SELECT prototype_locale_id FROM locales WHERE id = {locale_id}""")
      .on('locale_id -> locale).executeQuery()
      .as(SqlParser.str("prototype_locale_id").?.singleOpt) match {
        case Some(record) => Right(record)
        case None => Left(UndefinedLocale)
      }
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

  private lazy val foodRecordQuery = sqlFromResource("user/food_record.sql")

  private def foodRecordImpl(foodCode: String, locale: String, prototypeLocale: Option[String])(implicit conn: java.sql.Connection): Either[LocalFoodCodeError, FoodRow] = {
    val result = SQL(foodRecordQuery).on('food_code -> foodCode, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndFoodValidation(result, Macro.namedParser[FoodRow].single)()
  }

  private def foodCodeErrorAdapter[T](e: Either[FoodCodeError, T]): Either[LocalFoodCodeError, T] = e.left.map {
    case UndefinedCode => UndefinedCode
  }

  private def localeErrorAdapter[T](e: Either[LocaleError, T]): Either[LocalFoodCodeError, T] = e.left.map {
    case UndefinedLocale => UndefinedLocale
  }

  def foodData(foodCode: String, locale: String): Either[LocalFoodCodeError, (UserFoodData, FoodDataSources)] = tryWithConnection {
    implicit conn =>
      for (
        pl <- localeErrorAdapter(prototypeLocale(locale)).right;
        psm <- resolvePortionSizeMethods(foodCode, locale, pl).right;
        attr <- foodCodeErrorAdapter(resolveInheritableAttributes(foodCode)).right;
        nutr <- resolveNutrientTableCodes(foodCode, locale, pl).right;
        foodRow <- foodRecordImpl(foodCode, locale, pl).right
      ) yield {
        val localDescription = foodRow.local_description.orElse(foodRow.prototype_description).getOrElse(foodRow.english_description)
        val localDescriptionSource = if (foodRow.local_description.isEmpty && foodRow.prototype_description.nonEmpty)
          SourceLocale.Prototype(pl.get)
        else
          SourceLocale.Current(locale)

        (UserFoodData(foodRow.code, localDescription, nutr.codes, foodRow.food_group_id.toInt, psm.methods,
          attr.readyMealOption, attr.sameAsBeforeOption, attr.reasonableAmount),
          FoodDataSources(localDescriptionSource, nutr.sourceLocale, (psm.sourceLocale, psm.sourceRecord), attr.sources))
      }
  }
}
