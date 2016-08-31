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

  def configure() = {
    bind(classOf[DataStoreScala]).to(classOf[DataStoreSqlImpl])
    
    // Utility services
    
    bind(classOf[EnglishWordStemmer]).to(classOf[EnglishStemmerPlingImpl])
    
    // Basic admin services -- uncached for now
    
    bind(classOf[AsServedImageAdminService]).to(classOf[AsServedImageAdminImpl])
    bind(classOf[AssociatedFoodsAdminService]).to(classOf[AssociatedFoodsAdminImpl])
    bind(classOf[CategoriesAdminService]).to(classOf[CategoriesAdminImpl])
    bind(classOf[DrinkwareAdminService]).to(classOf[DrinkwareAdminImpl])
    bind(classOf[FoodBrowsingAdminService]).to(classOf[FoodBrowsingAdminImpl])
    bind(classOf[FoodGroupsAdminService]).to(classOf[FoodGroupsAdminImpl])
    bind(classOf[FoodsAdminService]).to(classOf[FoodsAdminImpl])
    bind(classOf[GuideImageAdminService]).to(classOf[GuideImageAdminImpl])
    bind(classOf[LocalesAdminService]).to(classOf[LocalesAdminImpl])
    bind(classOf[NutrientTablesAdminService]).to(classOf[NutrientTablesAdminImpl])
    
    // Observable admin services for higher-level cached services
    
    bind(classOf[ObservableFoodsAdminService]).to(classOf[ObservableFoodsAdminServiceImpl])
    bind(classOf[ObservableCategoriesAdminService]).to(classOf[ObservableCategoriesAdminServiceImpl])
    bind(classOf[ObservableLocalesAdminService]).to(classOf[ObservableLocalesAdminServiceImpl])
    
    // Admin services -- cached
    
    bind(classOf[ProblemCheckerService]).to(classOf[CachedProblemChecker])
    
    // Food index service
    
    bind(classOf[FoodIndexDataService]).to(classOf[FoodIndexDataImpl])
    
    // User food database service
    
    bind(classOf[FoodDatabaseService]).to(classOf[FoodDatabaseUserImpl])
       
  }
}
