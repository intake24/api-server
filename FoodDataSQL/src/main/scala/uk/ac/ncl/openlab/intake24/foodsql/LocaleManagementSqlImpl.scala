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

import uk.ac.ncl.openlab.intake24.services.LocaleManagementService
import javax.sql.DataSource
import com.google.inject.Inject
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.Locale
import anorm._
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale

class LocaleManagementSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends LocaleManagementService with SqlDataService {

  private case class LocaleRow(id: String, english_name: String, local_name: String, respondent_language_id: String, admin_language_id: String, country_flag_code: String, prototype_locale_id: Option[String]) {
    def mkLocale = Locale(id, english_name, local_name, respondent_language_id, admin_language_id, country_flag_code, prototype_locale_id)
  }

  def allLocales(): Either[DatabaseError, Seq[Locale]] = tryWithConnection {
    implicit conn =>

      var query = """SELECT id, english_name, local_name, respondent_language_id, admin_language_id, country_flag_code, prototype_locale_id FROM locales ORDER BY english_name"""

      Right(SQL(query).executeQuery().as(Macro.namedParser[LocaleRow].*).map(_.mkLocale))
  }

  def locale(id: String): Either[LocaleError, Locale] = tryWithConnection {
    implicit conn =>
      var query = """SELECT id, english_name, local_name, respondent_language_id, admin_language_id, country_flag_code, prototype_locale_id FROM locales WHERE id = {locale_id} ORDER BY english_name"""

      SQL(query).on('locale_id -> id).executeQuery().as(Macro.namedParser[LocaleRow].singleOpt).map(_.mkLocale) match {
        case Some(locale) => Right(locale)
        case None => Left(UndefinedLocale)
      }
  }

  def createLocale(data: Locale): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      var query = """INSERT INTO locales VALUES({id}, {english_name}, {local_name}, {respondent_language_code}, {admin_language_code}, {country_flag_code}, {prototype_locale_id})"""

      SQL(query).on('id -> data.id, 'english_name -> data.englishName, 'local_name -> data.localName, 'respondent_language_code -> data.respondentLanguage,
        'admin_language_code -> data.adminLanguage, 'country_flag_code -> data.flagCode, 'prototype_locale_id -> data.prototypeLocale).execute()

      Right(())
  }

  def updateLocale(id: String, data: Locale): Either[LocaleError, Unit] = tryWithConnection {
    implicit conn =>
      var query = """UPDATE locales SET id={new_id}, english_name={english_name}, local_name={local_name}, respondent_language_code={respondent_language_code}, admin_language_code={admin_language_code}, country_flag_code={country_flag_code}, prototype_locale_id={prototype_locale_id} WHERE id = {id}"""

      val updatedRows = SQL(query).on('id -> id, 'new_id -> data.id, 'english_name -> data.englishName, 'local_name -> data.localName, 'respondent_language_code -> data.respondentLanguage,
        'admin_language_code -> data.adminLanguage, 'country_flag_code -> data.flagCode, 'prototype_locale_id -> data.prototypeLocale).executeUpdate()
        
      if (updatedRows == 0)
        Left(UndefinedLocale)
      else
        Right(())
  }

  def deleteLocale(id: String): Either[LocaleError, Unit] = tryWithConnection {
    implicit conn =>
      val query = """DELETE FROM locales WHERE id={id}"""

      val updatedRows = SQL(query).on('id -> id).executeUpdate()
      
      if (updatedRows == 0)
        Left(UndefinedLocale)
      else
        Right(())
  }
}
