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

package uk.ac.ncl.openlab.intake24.foodsql.modular

import java.sql.Connection

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, sqlToSimple}
import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

trait LocaleQueries extends SqlDataService {

  protected case class LocaleRow(id: String, english_name: String, local_name: String, respondent_language_id: String, admin_language_id: String, country_flag_code: String, prototype_locale_id: Option[String], text_direction: String) {
    def mkLocale = Locale(id, english_name, local_name, respondent_language_id, admin_language_id, country_flag_code, prototype_locale_id, text_direction)
  }

  def listLocalesQuery()(implicit connection: Connection): Either[UnexpectedDatabaseError, Map[String, Locale]] = {
    val query = """SELECT id, english_name, local_name, respondent_language_id, admin_language_id, country_flag_code, prototype_locale_id, text_direction FROM locales ORDER BY english_name"""

    Right(SQL(query).executeQuery().as(Macro.namedParser[LocaleRow].*).map(row => (row.id -> row.mkLocale)).toMap)
  }

  def getLocaleQuery(id: String)(implicit connection: Connection): Either[LookupError, Locale] = {
    val query = """SELECT id, english_name, local_name, respondent_language_id, admin_language_id, country_flag_code, prototype_locale_id, text_direction FROM locales WHERE id = {locale_id} ORDER BY english_name"""

    SQL(query).on('locale_id -> id).executeQuery().as(Macro.namedParser[LocaleRow].singleOpt).map(_.mkLocale) match {
      case Some(locale) => Right(locale)
      case None => Left(RecordNotFound(new RuntimeException(id)))
    }
  }

  /**
    * Is translation for descriptions, associated food prompts etc. strictly required for this locale
    * (false if it can be inherited from the prototype locale)
    */
  def isTranslationRequiredQuery(localeId: String)(implicit connection: Connection): Either[LookupError, Boolean] =
    getLocaleQuery(localeId).right.flatMap {
      currentLocale =>
        currentLocale.prototypeLocale match {
          case Some(prototypeLocaleCode) => getLocaleQuery(prototypeLocaleCode).right.map {
            prototypeLocale =>
              currentLocale.respondentLanguage != prototypeLocale.respondentLanguage
          }
          case None => Right(true)
        }
    }
}
