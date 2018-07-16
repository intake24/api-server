package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import scala.concurrent.ExecutionContext.Implicits.global

import java.io.File
import java.sql.DriverManager
import com.typesafe.config.ConfigFactory
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.PairwiseAssociationsServiceConfiguration
import uk.ac.ncl.openlab.intake24.systemsql.admin.{DataExportImpl, SurveyAdminImpl}
import scala.collection.JavaConverters._

/**
  * Created by Tim Osadchiy on 12/07/2018.
  */
object PairwiseAssociationsServiceImplApp extends App {

  val configPath = "systemDataSql/src/test/test.conf"

  DriverManager.registerDriver(new org.postgresql.Driver)

  val conf = ConfigFactory.parseFile(new File(configPath))
  val dataSource = new org.postgresql.ds.PGSimpleDataSource()

  dataSource.setUser(conf.getString("db.username"))
  dataSource.setUrl(conf.getString("db.url"))
  dataSource.setPassword(conf.getString("db.password"))

  val settings = PairwiseAssociationsServiceConfiguration(
    conf.getInt("pairwiseAssociations.minimumNumberOfSurveySubmissions"),
    conf.getStringList("pairwiseAssociations.ignoreSurveysContaining").asScala,
    conf.getInt("pairwiseAssociations.useAfterNumberOfTransactions"),
    conf.getInt("pairwiseAssociations.rulesUpdateBatchSize"),
    conf.getString("pairwiseAssociations.refreshAtTime"),
    conf.getInt("pairwiseAssociations.minInputSearchSize")
  )
  val dataService = new PairwiseAssociationsDataServiceImpl(dataSource)
  val surveyAdminService = new SurveyAdminImpl(dataSource)
  val dataExportService = new DataExportImpl(dataSource)

  val service = new PairwiseAssociationsServiceImpl(settings, dataService, surveyAdminService, dataExportService)

  service.buildRules().onComplete { rules =>
    rules
  }

}
