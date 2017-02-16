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

import cache._
import com.google.inject.{AbstractModule, Injector, Provides, Singleton}
import com.google.inject.name.Named
import play.api.{Configuration, Environment, Logger}
import play.api.db.Database
import play.db.NamedDatabase
import uk.ac.ncl.openlab.intake24.datastoresql.{DataStoreScala, DataStoreSqlImpl}
import uk.ac.ncl.openlab.intake24.foodsql.NutrientMappingServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin._
import uk.ac.ncl.openlab.intake24.foodsql.demographicGroups._
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.foodsql.images.ImageDatabaseServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.{FoodDataUserStandaloneImpl, FoodDatabaseUserImpl}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups._
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{FoodDataService, FoodDatabaseService}
import uk.ac.ncl.openlab.intake24.services.foodindex.{FoodIndex, FoodIndexDataService}
import uk.ac.ncl.openlab.intake24.services.foodindex.english.{EnglishWordOps, EnglishWordOpsPlingImpl, FoodIndexImpl_en_GB}
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.UserAdminService
import uk.ac.ncl.openlab.intake24.systemsql.admin.UserAdminImpl

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

    val source = SourceImageSettings(
      configuration.getInt("intake24.images.processor.source.thumbnailWidth").get,
      configuration.getInt("intake24.images.processor.source.thumbnailHeight").get)

    val asServed = AsServedImageSettings(
      configuration.getInt("intake24.images.processor.asServed.mainImageWidth").get,
      configuration.getInt("intake24.images.processor.asServed.mainImageHeight").get,
      configuration.getInt("intake24.images.processor.asServed.thumbnailWidth").get)

    val selection = SelectionImageSettings(
      configuration.getInt("intake24.images.processor.selectionScreen.width").get,
      configuration.getInt("intake24.images.processor.selectionScreen.height").get)

    val imageMaps = ImageMapSettings(
      configuration.getInt("intake24.images.processor.imageMaps.baseImageWidth").get,
      configuration.getDouble("intake24.images.processor.imageMaps.outlineStrokeWidth").get,
      (configuration.getDouble("intake24.images.processor.imageMaps.outlineColor.r").get,
        configuration.getDouble("intake24.images.processor.imageMaps.outlineColor.g").get,
        configuration.getDouble("intake24.images.processor.imageMaps.outlineColor.b").get),
      configuration.getDouble("intake24.images.processor.imageMaps.outlineBlurStrength").get)

    val commandSearchPath = configuration.getString("intake24.images.processor.commandSearchPath")

    ImageProcessorSettings(commandSearchPath, source, selection, asServed, imageMaps)
  }

  def configure() = {
    bind(classOf[DataStoreScala]).to(classOf[DataStoreSqlImpl])

    // Utility services

    bind(classOf[EnglishWordOps]).to(classOf[EnglishWordOpsPlingImpl])

    // Basic admin services -- uncached

    bind(classOf[CategoriesAdminService]).annotatedWith(classOf[BasicImpl]).to(classOf[CategoriesAdminStandaloneImpl])
    bind(classOf[FoodsAdminService]).annotatedWith(classOf[BasicImpl]).to(classOf[FoodsAdminStandaloneImpl])
    bind(classOf[LocalesAdminService]).annotatedWith(classOf[BasicImpl]).to(classOf[LocalesAdminStandaloneImpl])

    bind(classOf[CategoriesAdminService]).to(classOf[ObservableCategoriesAdminServiceImpl])
    bind(classOf[FoodsAdminService]).to(classOf[ObservableFoodsAdminServiceImpl])
    bind(classOf[LocalesAdminService]).to(classOf[ObservableLocalesAdminServiceImpl])

    bind(classOf[UserAdminService]).to(classOf[UserAdminImpl])

    // User facing services

    bind(classOf[AsServedSetsAdminService]).to(classOf[AsServedSetsAdminStandaloneImpl])
    bind(classOf[AssociatedFoodsAdminService]).to(classOf[AssociatedFoodsAdminStandaloneImpl])
    bind(classOf[DrinkwareAdminService]).to(classOf[DrinkwareAdminStandaloneImpl])
    bind(classOf[FoodBrowsingAdminService]).to(classOf[FoodBrowsingAdminStandaloneImpl])
    bind(classOf[FoodGroupsAdminService]).to(classOf[FoodGroupsAdminStandaloneImpl])
    bind(classOf[GuideImageAdminService]).to(classOf[GuideImageAdminStandaloneImpl])
    bind(classOf[NutrientTablesAdminService]).to(classOf[NutrientTablesAdminStandaloneImpl])
    bind(classOf[QuickSearchService]).to(classOf[QuickSearchAdminStandaloneImpl])
    bind(classOf[ImageMapsAdminService]).to(classOf[ImageMapsAdminStandaloneImpl])
    bind(classOf[NutrientMappingService]).to(classOf[NutrientMappingServiceSqlImpl])

    // Observable admin services for higher-level cached services

    bind(classOf[ObservableFoodsAdminService]).to(classOf[ObservableFoodsAdminServiceImpl])
    bind(classOf[ObservableCategoriesAdminService]).to(classOf[ObservableCategoriesAdminServiceImpl])
    bind(classOf[ObservableLocalesAdminService]).to(classOf[ObservableLocalesAdminServiceImpl])

    bind(classOf[ImageDatabaseService]).to(classOf[ImageDatabaseServiceSqlImpl])
    bind(classOf[ImageAdminService]).to(classOf[ImageAdminServiceDefaultImpl])
    bind(classOf[FileTypeAnalyzer]).to(classOf[FileCommandFileTypeAnalyzer])
    bind(classOf[ImageProcessor]).to(classOf[ImageProcessorIM])

    // Admin services -- cached

    bind(classOf[ProblemCheckerService]).to(classOf[CachedProblemChecker])

    // Food index service

    bind(classOf[FoodIndexDataService]).to(classOf[FoodIndexDataImpl])

    // User food database service

    bind(classOf[FoodDatabaseService]).to(classOf[FoodDatabaseUserImpl])
    bind(classOf[FoodDataService]).to(classOf[FoodDataUserStandaloneImpl])


    // Demographic service
    bind(classOf[DemographicGroupsService]).to(classOf[DemographicGroupsServiceImpl])


  }
}
