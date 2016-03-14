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

import org.slf4j.LoggerFactory

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import javax.sql.DataSource
import net.scran24.fooddef.UserCategoryHeader
import net.scran24.fooddef.UserFoodHeader
import uk.ac.ncl.openlab.intake24.services.IndexFoodDataService

@Singleton
class IndexFoodDataServiceSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends SqlDataService 
  with IndexFoodDataService with SplitterDataSqlImpl with SynsetsDataSqlImpl {

  val logger = LoggerFactory.getLogger(classOf[IndexFoodDataServiceSqlImpl])
    
  def indexableFoods(locale: String): Seq[UserFoodHeader] = tryWithConnection {
    implicit conn =>
      val query =
        """|SELECT code, COALESCE(t1.local_description, t2.local_description) AS local_description
           |FROM foods
           |LEFT JOIN foods_local as t1 ON foods.code = t1.food_code AND t1.locale_id = {locale_id}
           |LEFT JOIN foods_local as t2 ON foods.code = t2.food_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id})
           |LEFT JOIN foods_restrictions ON foods.code = foods_restrictions.food_code
           |WHERE 
           |(t1.local_description IS NOT NULL OR t2.local_description IS NOT NULL) 
           |AND (foods_restrictions.locale_id = {locale_id} OR foods_restrictions.locale_id IS NULL)
           |ORDER BY local_description""".stripMargin
           
      SQL(query).on('locale_id -> locale).executeQuery().as(Macro.indexedParser[UserFoodHeader].*)
  }

  def indexableCategories(locale: String): Seq[UserCategoryHeader] = tryWithConnection {
    implicit conn =>
      val query =
        """|SELECT code, COALESCE(t1.local_description, t2.local_description) AS local_description
           |FROM categories
           |LEFT JOIN categories_local as t1 ON categories.code = t1.category_code AND t1.locale_id = {locale_id}
           |LEFT JOIN categories_local as t2 ON categories.code = t2.category_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id})
           |LEFT JOIN categories_restrictions ON categories.code = categories_restrictions.category_code
           |WHERE 
           |(t1.local_description IS NOT NULL OR t2.local_description IS NOT NULL) 
           |AND (categories_restrictions.locale_id = {locale_id} OR categories_restrictions.locale_id IS NULL)
           |ORDER BY local_description""".stripMargin

      SQL(query).on('locale_id -> locale).executeQuery().as(Macro.indexedParser[UserCategoryHeader].*)
  }
}
