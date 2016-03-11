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
import net.scran24.fooddef.Locale
import anorm._

class LocaleManagementSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends LocaleManagementService with SqlDataService {

  private case class LocaleRow(id: String, english_name: String, local_name: String, respondent_language_code: String, admin_language_code: String, country_flag_code: String, prototype_locale_id: Option[String]) {
    def mkLocale = Locale(id, english_name, local_name, respondent_language_code, admin_language_code, country_flag_code, prototype_locale_id)
  }

  def list: Seq[Locale] = tryWithConnection {
    implicit conn =>

      var query = """SELECT id, english_name, local_name, respondent_language_code, admin_language_code, country_flag_code, prototype_locale_id FROM locales ORDER BY english_name"""

      SQL(query).executeQuery().as(Macro.namedParser[LocaleRow].*).map(_.mkLocale)
  }

  def get(id: String): Option[Locale] = tryWithConnection {
    implicit conn =>
      var query = """SELECT id, english_name, local_name, respondent_language_code, admin_language_code, country_flag_code, prototype_locale_id FROM locales WHERE id = {locale_id} ORDER BY english_name"""

      SQL(query).on('locale_id -> id).executeQuery().as(Macro.namedParser[LocaleRow].singleOpt).map(_.mkLocale)
  }

  def create(data: Locale) = tryWithConnection {
    implicit conn =>
      var query = """INSERT INTO locales VALUES({id}, {english_name}, {local_name}, {respondent_language_code}, {admin_language_code}, {country_flag_code}, {prototype_locale_id})"""

      SQL(query).on('id -> data.id, 'english_name -> data.englishName, 'local_name -> data.localName, 'respondent_language_code -> data.respondentLanguage,
        'admin_language_code -> data.adminLanguage, 'country_flag_code -> data.flagCode, 'prototype_locale_id -> data.prototypeLocale).execute()

  }

  def update(id: String, data: Locale) = tryWithConnection {
    implicit conn =>
      var query = """UPDATE locales SET id={new_id}, english_name={english_name}, local_name={local_name}, respondent_language_code={respondent_language_code}, admin_language_code={admin_language_code}, country_flag_code={country_flag_code}, prototype_locale_id={prototype_locale_id} WHERE id = {id}"""

      SQL(query).on('id -> id, 'new_id -> data.id, 'english_name -> data.englishName, 'local_name -> data.localName, 'respondent_language_code -> data.respondentLanguage,
        'admin_language_code -> data.adminLanguage, 'country_flag_code -> data.flagCode, 'prototype_locale_id -> data.prototypeLocale).execute()

  }

  def delete(id: String) = tryWithConnection {
    implicit conn =>
      val query = """DELETE FROM locales WHERE id={id}"""
      
      SQL(query).on('id -> id).execute()
  }
}
