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
import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Injector, Provides, Singleton}
import play.api.db.Database
import play.api.{Configuration, Environment}
import play.db.NamedDatabase
import uk.ac.ncl.openlab.intake24.datastoresql.{DataStoreScala, DataStoreSqlImpl}
import uk.ac.ncl.openlab.intake24.foodsql.NutrientMappingServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin._
import uk.ac.ncl.openlab.intake24.foodsql.demographicGroups._
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.foodsql.images.ImageDatabaseServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.user._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.demographicgroups._
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import uk.ac.ncl.openlab.intake24.services.fooddb.user._
import uk.ac.ncl.openlab.intake24.services.foodindex.danish.{FoodIndexImpl_da_DK, SplitterImpl_da_DK}
import uk.ac.ncl.openlab.intake24.services.foodindex.english.{EnglishWordOps, EnglishWordOpsPlingImpl, FoodIndexImpl_en_GB, SplitterImpl_en_GB}
import uk.ac.ncl.openlab.intake24.services.foodindex.portuguese.{FoodIndexImpl_pt_PT, SplitterImpl_pt_PT}
import uk.ac.ncl.openlab.intake24.services.foodindex.{FoodIndex, FoodIndexDataService, Splitter}
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, SurveyAdminService, UserAdminService}
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{FoodPopularityService, SurveyService}
import uk.ac.ncl.openlab.intake24.systemsql.admin.{DataExportImpl, SurveyAdminImpl, UserAdminImpl}
import uk.ac.ncl.openlab.intake24.systemsql.user.{FoodPopularityServiceImpl, SurveyServiceImpl}


class Intake24ServicesModule(env: Environment, config: Configuration) extends AbstractModule {
  @Provides
  @Singleton
  def foodIndexes(injector: Injector): Map[String, FoodIndex] =
    Map("en_GB" -> injector.getInstance(classOf[FoodIndexImpl_en_GB]),
      "pt_PT" -> injector.getInstance(classOf[FoodIndexImpl_pt_PT]),
      "da_DK" -> injector.getInstance(classOf[FoodIndexImpl_da_DK]))

  @Provides
  @Singleton
  def foodDescriptionSplitters(injector: Injector): Map[String, Splitter] =
    Map("en_GB" -> injector.getInstance(classOf[SplitterImpl_en_GB]),
      "pt_PT" -> injector.getInstance(classOf[SplitterImpl_pt_PT]),
      "da_DK" -> injector.getInstance(classOf[SplitterImpl_da_DK]))

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
    bind(classOf[SurveyAdminService]).to(classOf[SurveyAdminImpl])
    bind(classOf[DataExportService]).to(classOf[DataExportImpl])

    // User facing services

    bind(classOf[AsServedSetsAdminService]).to(classOf[AsServedSetsAdminImpl])
    bind(classOf[AssociatedFoodsAdminService]).to(classOf[AssociatedFoodsAdminImpl])
    bind(classOf[DrinkwareAdminService]).to(classOf[DrinkwareAdminImpl])
    bind(classOf[FoodBrowsingAdminService]).to(classOf[FoodBrowsingAdminImpl])
    bind(classOf[FoodGroupsAdminService]).to(classOf[FoodGroupsAdminImpl])
    bind(classOf[GuideImageAdminService]).to(classOf[GuideImageAdminImpl])
    bind(classOf[NutrientTablesAdminService]).to(classOf[NutrientTablesAdminImpl])
    bind(classOf[QuickSearchService]).to(classOf[QuickSearchAdminImpl])
    bind(classOf[ImageMapsAdminService]).to(classOf[ImageMapsAdminImpl])
    bind(classOf[NutrientMappingService]).to(classOf[NutrientMappingServiceSqlImpl])

    bind(classOf[SurveyService]).to(classOf[SurveyServiceImpl])

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

    bind(classOf[FoodDataService]).to(classOf[FoodDataServiceImpl])
    bind(classOf[FoodPopularityService]).to(classOf[FoodPopularityServiceImpl])
    bind(classOf[FoodBrowsingService]).to(classOf[FoodBrowsingServiceImpl])
    bind(classOf[AsServedSetsService]).to(classOf[AsServedSetsServiceImpl])
    bind(classOf[GuideImageService]).to(classOf[GuideImageServiceImpl])
    bind(classOf[AssociatedFoodsService]).to(classOf[AssociatedFoodsServiceImpl])
    bind(classOf[DrinkwareService]).to(classOf[DrinkwareServiceImpl])
    bind(classOf[BrandNamesService]).to(classOf[BrandNamesServiceImpl])
    bind(classOf[ImageMapService]).to(classOf[ImageMapServiceImpl])


    // Demographic service
    bind(classOf[DemographicGroupsService]).to(classOf[DemographicGroupsServiceImpl])

  }
}
