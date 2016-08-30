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
import cache.CachedFoodDatabaseAdminService


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
    
    // Uncached internal food data services
    
    bind(classOf[FoodDatabaseService]).to(classOf[FoodDatabaseUserImpl])
    bind(classOf[FoodIndexDataService]).to(classOf[FoodIndexDataImpl])
    bind(classOf[FoodDatabaseAdminService]).annotatedWith(classOf[UncachedImpl]).to(classOf[FoodDatabaseAdminImpl])
    
    // User-facing (cached) food data services
    
    bind(classOf[FoodDatabaseAdminService]).to(classOf[CachedFoodDatabaseAdminService])
    bind(classOf[ProblemCheckerService]).to(classOf[CachedProblemChecker])
    
  }
}
