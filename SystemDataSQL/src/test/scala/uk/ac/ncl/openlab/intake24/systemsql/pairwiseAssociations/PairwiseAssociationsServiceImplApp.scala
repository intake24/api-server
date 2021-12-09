package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import java.io.File
import java.sql.DriverManager
import java.util.concurrent.ForkJoinPool

import com.typesafe.config.ConfigFactory
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.PairwiseAssociationsServiceConfiguration
import uk.ac.ncl.openlab.intake24.systemsql.admin.{DataExportImpl, SurveyAdminImpl}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

/**
  * Created by Tim Osadchiy on 12/07/2018.
  */
object PairwiseAssociationsServiceImplApp extends App {

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
  val context = ExecutionContext.fromExecutor(new ForkJoinPool(maxThreads))
  val dataService = new PairwiseAssociationsDataServiceImpl(dataSource, settings, context)
  val surveyAdminService = new SurveyAdminImpl(dataSource)
  val dataExportService = new DataExportImpl(dataSource)

  val service = new PairwiseAssociationsServiceImpl(settings, dataService, surveyAdminService, dataExportService, context)

  checkConsistance()

  def checkConsistance() = {
    /* val f = for (
      transactions <- EitherT(dataService.getAssociations());
      builtRules <- service.buildRules();
      dbRules <- service.getAssociationRulesAsync();
      equal = graphsEqual(builtRules, dbRules)
    ) yield equal
    f.onComplete { f =>
      f
    }
    Await.result(f, Duration.Inf) */
  }

  def buildRules() = {
    /* service.buildRules().onComplete { rules =>
      rules
    } */
  }

  def refresh() = {
    /* service.refresh().onComplete(graph => {
      graph
    }) */
  }

  def graphsEqual(builtGraph: Map[String, PairwiseAssociationRules], dbGraph: Map[String, PairwiseAssociationRules]) = {
    val reducedGraph = builtGraph.mapValues(_.reduce(settings.storedCoOccurrencesThreshold))
    reducedGraph.forall { node =>
      dbGraph.get(node._1) match {
        case Some(dbPa) => pairwiseAreEqual(node._2, dbPa)
        case None => false
      }
    }
  }

  def pairwiseAreEqual(pa1: PairwiseAssociationRules, pa2: PairwiseAssociationRules) = {
    val params1 = pa1.getParams()
    val params2 = pa2.getParams()

    params1.occurrences == params2.occurrences &&
      (params1.coOccurrences.toSet diff params2.coOccurrences.toSet).isEmpty &&
      (params1.occurrences.toSet diff params2.occurrences.toSet).isEmpty

  }

}
