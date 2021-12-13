package modules


import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides, Singleton}
import play.api.Configuration
import play.api.db.{Database, NamedDatabase}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, SurveyAdminService}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsDataService, PairwiseAssociationsService, PairwiseAssociationsServiceConfiguration}
import uk.ac.ncl.openlab.intake24.systemsql.admin.{DataExportImpl, SurveyAdminImpl}
import uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations.{PairwiseAssociationsDataServiceImpl, PairwiseAssociationsServiceImpl}

import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext

/*
  Minimal set of dependencies to enable testing of pairwise associations module
*/
class PairwiseAssociationsTestModule extends AbstractModule {

  @Provides
  @Named("intake24_system")
  def systemDataSource(@NamedDatabase("intake24_system") db: Database) = db.dataSource

  @Provides
  @Named("intake24_foods")
  def foodsDataSource(@NamedDatabase("intake24_foods") db: Database) = db.dataSource

  // Custom execution context for long-running blocking tasks (data export etc.)
  @Provides
  @Named("intake24")
  @Singleton
  def longTasksExecutionContext(configuration: Configuration): ExecutionContext = {
    val maxThreads = configuration.get[Int]("intake24.longTasksContext.maxThreads")
    ExecutionContext.fromExecutor(new ForkJoinPool(maxThreads))
  }

  @Provides
  @Singleton
  def pairwiseAssociationsServiceSettings(configuration: Configuration): PairwiseAssociationsServiceConfiguration =
    PairwiseAssociationsServiceConfiguration(
      configuration.get[Int]("intake24.pairwiseAssociations.minimumNumberOfSurveySubmissions"),
      configuration.get[Seq[String]]("intake24.pairwiseAssociations.ignoreSurveysContaining"),
      configuration.get[Int]("intake24.pairwiseAssociations.useAfterNumberOfTransactions"),
      configuration.get[Int]("intake24.pairwiseAssociations.rulesUpdateBatchSize"),
      configuration.get[String]("intake24.pairwiseAssociations.refreshAtTime"),
      configuration.get[Int]("intake24.pairwiseAssociations.minInputSearchSize"),
      configuration.get[Int]("intake24.pairwiseAssociations.readWriteRulesDbBatchSize"),
      configuration.get[Int]("intake24.pairwiseAssociations.storedCoOccurrencesThreshold")
    )


  override def configure() = {
    bind(classOf[PairwiseAssociationsDataService]).to(classOf[PairwiseAssociationsDataServiceImpl])
    bind(classOf[PairwiseAssociationsService]).to(classOf[PairwiseAssociationsServiceImpl])
    bind(classOf[SurveyAdminService]).to(classOf[SurveyAdminImpl])
    bind(classOf[DataExportService]).to(classOf[DataExportImpl])
  }
}
