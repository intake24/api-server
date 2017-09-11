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

package uk.ac.ncl.openlab.intake24.foodsql.admin

import javax.sql.DataSource

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, sqlToSimple}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.foodsql.modular.LocaleQueries
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.LocalesAdminService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

@Singleton
class LocalesAdminImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends LocalesAdminService with SqlDataService with LocaleQueries {

  def listLocales(): Either[UnexpectedDatabaseError, Map[String, Locale]] = tryWithConnection {
    implicit conn =>
      listLocalesQuery()
  }

  def getLocale(id: String): Either[LookupError, Locale] = tryWithConnection {
    implicit conn =>
      getLocaleQuery(id)
  }

  def createLocale(data: Locale): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      var query = """INSERT INTO locales VALUES({id}, {english_name}, {local_name}, {respondent_language_code}, {admin_language_code}, {country_flag_code}, {prototype_locale_id}, {text_dir})"""

      tryWithConstraintCheck("locales_pk", DuplicateCode) {
        SQL(query).on('id -> data.id, 'english_name -> data.englishName, 'local_name -> data.localName, 'respondent_language_code -> data.respondentLanguage,
          'admin_language_code -> data.adminLanguage, 'country_flag_code -> data.flagCode, 'prototype_locale_id -> data.prototypeLocale,
          'text_dir -> data.textDirection).execute()
        Right(())
      }
  }

  def updateLocale(id: String, data: Locale): Either[UpdateError, Unit] = tryWithConnection {
    implicit conn =>
      var query = """UPDATE locales SET id={new_id}, english_name={english_name}, local_name={local_name}, respondent_language_code={respondent_language_code}, admin_language_code={admin_language_code}, country_flag_code={country_flag_code}, prototype_locale_id={prototype_locale_id},text_direction={text_dir} WHERE id = {id}"""

      val updatedRows = SQL(query).on('id -> id, 'new_id -> data.id, 'english_name -> data.englishName, 'local_name -> data.localName, 'respondent_language_code -> data.respondentLanguage,
        'admin_language_code -> data.adminLanguage, 'country_flag_code -> data.flagCode, 'prototype_locale_id -> data.prototypeLocale,
        'text_dir -> data.textDirection).executeUpdate()

      if (updatedRows == 0)
        Left(RecordNotFound(new RuntimeException(id)))
      else
        Right(())
  }

  def deleteLocale(id: String): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      val query = """DELETE FROM locales WHERE id={id}"""

      val updatedRows = SQL(query).on('id -> id).executeUpdate()

      if (updatedRows == 0)
        Left(RecordNotFound(new RuntimeException(id)))
      else
        Right(())
  }

  /**
    * Is translation for descriptions, associated food prompts etc. strictly required for this locale
    * (false if it can be inherited from the prototype locale)
    */
  def isTranslationRequired(id: String): Either[LookupError, Boolean] = tryWithConnection {
    implicit conn =>
      isTranslationRequiredQuery(id)
  }
}
