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

package modules

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Named
import play.api.Configuration
import play.api.Environment
import play.api.db.Database
import play.db.NamedDatabase
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreScala
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreSqlImpl
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndex
import uk.ac.ncl.openlab.intake24.services.foodindex.english.EnglishStemmerPlingImpl
import uk.ac.ncl.openlab.intake24.services.foodindex.english.EnglishWordStemmer
import uk.ac.ncl.openlab.intake24.services.foodindex.english.FoodIndexImpl_en_GB
import java.lang.annotation.Retention
import com.google.inject.BindingAnnotation
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Annotation

import cache.CachedProblemChecker
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDatabaseService
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndexDataService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodDatabaseUserImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import uk.ac.ncl.openlab.intake24.foodsql.admin.AsServedImageAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.AssociatedFoodsAdminImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService
import uk.ac.ncl.openlab.intake24.foodsql.admin.CategoriesAdminImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.DrinkwareAdminService
import uk.ac.ncl.openlab.intake24.foodsql.admin.DrinkwareAdminImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodBrowsingAdminService
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodBrowsingAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodGroupsAdminImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodsAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.GuideImageAdminImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.GuideImageAdminService
import uk.ac.ncl.openlab.intake24.foodsql.admin.LocalesAdminImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.LocalesAdminService
import uk.ac.ncl.openlab.intake24.foodsql.admin.NutrientTablesAdminImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.NutrientTablesAdminService
import cache.ObservableFoodsAdminService
import cache.ObservableFoodsAdminServiceImpl
import cache.ObservableCategoriesAdminService
import cache.ObservableCategoriesAdminServiceImpl
import cache.ObservableLocalesAdminService
import cache.ObservableLocalesAdminServiceImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.LocalesAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.GuideImageAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodGroupsAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.CategoriesAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodBrowsingAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodsAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.DrinkwareAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.AsServedImageAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.AssociatedFoodsAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.NutrientTablesAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodDataUserStandaloneImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDataService
import uk.ac.ncl.openlab.intake24.foodsql.admin.QuickSearchAdminStandaloneImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.QuickSearchService
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageProcessor
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageProcessorIM
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageProcessorSettings
import uk.ac.ncl.openlab.intake24.services.fooddb.images.AsServedImageSettings
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageAdminServiceDefaultImpl
import uk.ac.ncl.openlab.intake24.foodsql.images.ImageDatabaseServiceSqlImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDatabaseService

class Intake24ServicesModule(env: Environment, config: Configuration) extends AbstractModule {
  @Provides
  @Singleton
  def foodIndexes(injector: Injector): Map[String, FoodIndex] =
    Map("en_GB" -> injector.getInstance(classOf[FoodIndexImpl_en_GB]))

  @Provides
  @Named("intake24_system")
  def systemDataSource(@NamedDatabase("intake24_system") db: Database) = db.dataSource

  @Provides
  @Named("intake24_foods")
  def foodsDataSource(@NamedDatabase("intake24_foods") db: Database) = db.dataSource

  @Provides
  @Singleton
  def imageProcessorSettings(configuration: Configuration): ImageProcessorSettings = {
    val asServed = AsServedImageSettings(
      configuration.getInt("intake24.images.processor.asServed.mainImageWidth").getOrElse(500),
      configuration.getInt("intake24.images.processor.asServed.mainImageHeight").getOrElse(500),
      configuration.getInt("intake24.images.processor.asServed.thumbnailWidth").getOrElse(500),
      configuration.getInt("intake24.images.processor.asServed.thumbnailHeight").getOrElse(500))

    ImageProcessorSettings(asServed)
  }

  def configure() = {
    bind(classOf[DataStoreScala]).to(classOf[DataStoreSqlImpl])

    // Utility services

    bind(classOf[EnglishWordStemmer]).to(classOf[EnglishStemmerPlingImpl])

    // Basic admin services -- uncached

    bind(classOf[CategoriesAdminService]).annotatedWith(classOf[BasicImpl]).to(classOf[CategoriesAdminStandaloneImpl])
    bind(classOf[FoodsAdminService]).annotatedWith(classOf[BasicImpl]).to(classOf[FoodsAdminStandaloneImpl])
    bind(classOf[LocalesAdminService]).annotatedWith(classOf[BasicImpl]).to(classOf[LocalesAdminStandaloneImpl])

    bind(classOf[CategoriesAdminService]).to(classOf[ObservableCategoriesAdminServiceImpl])
    bind(classOf[FoodsAdminService]).to(classOf[ObservableFoodsAdminServiceImpl])
    bind(classOf[LocalesAdminService]).to(classOf[ObservableLocalesAdminServiceImpl])

    // User facing services

    bind(classOf[AsServedImageAdminService]).to(classOf[AsServedImageAdminStandaloneImpl])
    bind(classOf[AssociatedFoodsAdminService]).to(classOf[AssociatedFoodsAdminStandaloneImpl])
    bind(classOf[DrinkwareAdminService]).to(classOf[DrinkwareAdminStandaloneImpl])
    bind(classOf[FoodBrowsingAdminService]).to(classOf[FoodBrowsingAdminStandaloneImpl])
    bind(classOf[FoodGroupsAdminService]).to(classOf[FoodGroupsAdminStandaloneImpl])
    bind(classOf[GuideImageAdminService]).to(classOf[GuideImageAdminStandaloneImpl])
    bind(classOf[NutrientTablesAdminService]).to(classOf[NutrientTablesAdminStandaloneImpl])
    bind(classOf[QuickSearchService]).to(classOf[QuickSearchAdminStandaloneImpl])

    // Observable admin services for higher-level cached services

    bind(classOf[ObservableFoodsAdminService]).to(classOf[ObservableFoodsAdminServiceImpl])
    bind(classOf[ObservableCategoriesAdminService]).to(classOf[ObservableCategoriesAdminServiceImpl])
    bind(classOf[ObservableLocalesAdminService]).to(classOf[ObservableLocalesAdminServiceImpl])

    
    bind(classOf[ImageDatabaseService]).to(classOf[ImageDatabaseServiceSqlImpl])
    bind(classOf[ImageAdminService]).to(classOf[ImageAdminServiceDefaultImpl])
    bind(classOf[ImageProcessor]).to(classOf[ImageProcessorIM])

    // Admin services -- cached

    bind(classOf[ProblemCheckerService]).to(classOf[CachedProblemChecker])

    // Food index service

    bind(classOf[FoodIndexDataService]).to(classOf[FoodIndexDataImpl])

    // User food database service

    bind(classOf[FoodDatabaseService]).to(classOf[FoodDatabaseUserImpl])
    bind(classOf[FoodDataService]).to(classOf[FoodDataUserStandaloneImpl])

  }
}
