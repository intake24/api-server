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

package uk.ac.ncl.openlab.intake24.services.dataexport.guice

import java.util.concurrent.ForkJoinPool

import com.google.inject.{AbstractModule, Provides}
import javax.inject.{Named, Singleton}
import org.http4s.Uri
import play.api.Configuration
import play.api.db.{Database, NamedDatabase}
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodGroupsAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.NdnsCompoundFoodGroupsImpl
import uk.ac.ncl.openlab.intake24.services.NdnsCompoundFoodGroupsService
import uk.ac.ncl.openlab.intake24.services.dataexport.{CSVFormatV1, CSVFormatV2, DataExportDaemon, SurveyCSVExporter}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, ScheduledDataExportService, SurveyAdminService, UserAdminService}
import uk.ac.ncl.openlab.intake24.shorturls.ShortUrlsHttpClientConfig
import uk.ac.ncl.openlab.intake24.systemsql.admin.{DataExportImpl, ScheduledDataExportImpl, SurveyAdminImpl, UserAdminImpl}

import scala.concurrent.ExecutionContext

class DataExportModule extends AbstractModule {

  override def configure() = {

    bind(classOf[UserAdminService]).to(classOf[UserAdminImpl])
    bind(classOf[SurveyAdminService]).to(classOf[SurveyAdminImpl])

    bind(classOf[DataExportService]).to(classOf[DataExportImpl])
    bind(classOf[ScheduledDataExportService]).to(classOf[ScheduledDataExportImpl])

    bind(classOf[FoodGroupsAdminService]).to(classOf[FoodGroupsAdminImpl])
    bind(classOf[NdnsCompoundFoodGroupsService]).to(classOf[NdnsCompoundFoodGroupsImpl])

    bind(classOf[DataExportDaemon]).asEagerSingleton()
  }

  @Provides
  @Singleton
  def shortUrlsConfig(configuration: Configuration): ShortUrlsHttpClientConfig = {
    ShortUrlsHttpClientConfig(Uri.unsafeFromString(configuration.get[String]("intake24.shortUrlServiceUrl")))
  }


  @Provides
  @Singleton
  def csvExportFormats(): Map[String, SurveyCSVExporter] =
    Map("v1" -> new SurveyCSVExporter(new CSVFormatV1), "v2" -> new SurveyCSVExporter(new CSVFormatV2))


  // Custom execution context for long-running blocking tasks (data export etc.)
  @Provides
  @Named("intake24")
  @Singleton
  def createThreadPool(configuration: Configuration): ExecutionContext = {
    val maxThreads = configuration.get[Int]("intake24.threadPool.maxThreads")
    ExecutionContext.fromExecutor(new ForkJoinPool(maxThreads))
  }

  @Provides
  @Named("intake24_system")
  def systemDataSource(@NamedDatabase("intake24_system") db: Database) = db.dataSource

  @Provides
  @Named("intake24_foods")
  def foodsDataSource(@NamedDatabase("intake24_foods") db: Database) = db.dataSource

}
