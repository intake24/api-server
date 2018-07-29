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

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, SqlParser, sqlToSimple}
import com.google.inject.Singleton
import com.google.inject.name.Named
import javax.inject.Inject
import javax.sql.DataSource
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.data.UserFoodHeader
import uk.ac.ncl.openlab.intake24.api.data.admin.FoodHeader
import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, LocaleError, UndefinedLocale}
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{FoodDataService, FoodDataSources, ResolvedFoodData, SourceLocale}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

@Singleton
class FoodDataServiceImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends FoodDataService
  with SqlDataService
  with SqlResourceLoader
  with FirstRowValidation
  with InheritedAttributesImpl
  with InheritedPortionSizeMethodsImpl
  with InheritedNutrientTableCodesImpl {

  val logger = LoggerFactory.getLogger(classOf[FoodDataServiceImpl])

  private case class FoodRow(food_code: String, english_description: String, local_description: Option[String], prototype_description: Option[String], food_group_id: Long)

  private def prototypeLocale(locale: String)(implicit conn: java.sql.Connection): Either[LocaleError, Option[String]] = {
    SQL("""SELECT prototype_locale_id FROM locales WHERE id = {locale_id}""")
      .on('locale_id -> locale).executeQuery()
      .as(SqlParser.str("prototype_locale_id").?.singleOpt) match {
      case Some(record) => Right(record)
      case None => Left(UndefinedLocale(new RuntimeException(s"Locale $locale undefined")))
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

  private def foodRecordImpl(foodCode: String, locale: String, prototypeLocale: Option[String])(implicit conn: java.sql.Connection): Either[LocalLookupError, FoodRow] = {
    val result = SQL(foodRecordQuery).on('food_code -> foodCode, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndFoodValidation(foodCode, result, Macro.namedParser[FoodRow].single)()
  }

  def getFoodData(foodCode: String, locale: String): Either[LocalLookupError, (ResolvedFoodData, FoodDataSources)] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          pl <- prototypeLocale(locale).right;
          psm <- resolvePortionSizeMethods(foodCode, locale, pl).right;
          attr <- resolveInheritableAttributes(foodCode).right;
          nutr <- resolveNutrientTableCodes(foodCode, locale, pl).right;
          foodRow <- foodRecordImpl(foodCode, locale, pl).right
        ) yield {
          val localDescription = foodRow.local_description.orElse(foodRow.prototype_description).getOrElse(foodRow.english_description)
          val localDescriptionSource = if (foodRow.local_description.isEmpty && foodRow.prototype_description.nonEmpty)
            SourceLocale.Prototype(pl.get)
          else
            SourceLocale.Current(locale)

          (ResolvedFoodData(foodRow.food_code, foodRow.english_description, localDescription, foodRow.food_group_id.toInt, attr.reasonableAmount, attr.readyMealOption, attr.sameAsBeforeOption, nutr.codes, psm.methods),
            FoodDataSources(localDescriptionSource, nutr.sourceLocale, (psm.sourceLocale, psm.sourceRecord), attr.sources))
        }
      }
  }

  override def getFoodHeader(code: String, localeId: String): Either[LocalLookupError, UserFoodHeader] = tryWithConnection {
    implicit conn =>
      val r = SQL(
        """
          |SELECT
          |  f.code,
          |  f2.local_description as localDescription
          |FROM foods f
          |  JOIN foods_local f2 on f.code = f2.food_code
          |WHERE f.code = {code} AND f2.locale_id={localeId};
        """.stripMargin).on('code -> code, 'localeId -> localeId).executeQuery()
        .as(Macro.namedParser[UserFoodHeader].single)
      Right(r)

  }

}
