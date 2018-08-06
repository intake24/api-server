package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import java.io.File
import java.sql.DriverManager
import java.util.concurrent.ForkJoinPool

import com.typesafe.config.ConfigFactory
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.PairwiseAssociationsServiceConfiguration
import uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations.PairwiseAssociationsServiceImplApp.conf

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

/**
  * Created by Tim Osadchiy on 12/07/2018.
  */
object PairwiseAssociationsDataServiceImplApp extends App {

  val configPath = "apiPlayServer/conf/application.conf"

  DriverManager.registerDriver(new org.postgresql.Driver)

  val conf = ConfigFactory.parseFile(new File(configPath))
  val dataSource = new org.postgresql.ds.PGSimpleDataSource()

  dataSource.setUser(conf.getString("db.intake24_system.username"))
  dataSource.setUrl(conf.getString("db.intake24_system.url"))
  dataSource.setPassword(conf.getString("db.intake24_system.password"))

  val settings = PairwiseAssociationsServiceConfiguration(
    conf.getInt("intake24.pairwiseAssociations.minimumNumberOfSurveySubmissions"),
    conf.getStringList("intake24.pairwiseAssociations.ignoreSurveysContaining").asScala,
    conf.getInt("intake24.pairwiseAssociations.useAfterNumberOfTransactions"),
    conf.getInt("intake24.pairwiseAssociations.rulesUpdateBatchSize"),
    conf.getString("intake24.pairwiseAssociations.refreshAtTime"),
    conf.getInt("intake24.pairwiseAssociations.minInputSearchSize"),
    conf.getInt("intake24.pairwiseAssociations.readWriteRulesDbBatchSize"),
    conf.getInt("intake24.pairwiseAssociations.storedCoOccurrencesThreshold")
  )
  val maxThreads = conf.getInt("intake24.longTasksContext.maxThreads")
  implicit val context = ExecutionContext.fromExecutor(new ForkJoinPool(maxThreads))
  val paDataService = new PairwiseAssociationsDataServiceImpl(dataSource, settings, context)

  def getAssociations() = {
    paDataService.getAssociations().onComplete { as =>
      as
    }
  }

}
