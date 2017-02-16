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

package uk.ac.ncl.openlab.intake24.foodsql.foodindex

import javax.sql.DataSource

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, sqlToSimple}
import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.{UserCategoryHeader, UserFoodHeader}
import uk.ac.ncl.openlab.intake24.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndexDataService
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}


@Singleton
class FoodIndexDataImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends SqlDataService
  with SqlResourceLoader with FoodIndexDataService with FoodIndexDataSharedImpl {

  private val logger = LoggerFactory.getLogger(classOf[FoodIndexDataImpl])

  private val indexableFoodsQuery = sqlFromResource("foodindex/indexable_foods.sql")

  def indexableFoods(locale: String): Either[LocaleError, Seq[UserFoodHeader]] = tryWithConnection {
    implicit conn =>
      Right(SQL(indexableFoodsQuery).on('locale_id -> locale).executeQuery().as(Macro.indexedParser[UserFoodHeader].*))
  }

  private val indexableCategoriesQuery = sqlFromResource("foodindex/indexable_categories.sql")

  def indexableCategories(locale: String): Either[LocaleError, Seq[UserCategoryHeader]] = tryWithConnection {
    implicit conn =>
      Right(SQL(indexableCategoriesQuery).on('locale_id -> locale).executeQuery().as(Macro.indexedParser[UserCategoryHeader].*))
  }
}
