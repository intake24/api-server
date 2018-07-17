package uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations

import scala.concurrent.ExecutionContext.Implicits.global

import java.io.File
import java.sql.DriverManager
import com.typesafe.config.ConfigFactory

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

  val paDataService = new PairwiseAssociationsDataServiceImpl(dataSource, 2000000)
  paDataService.getAssociations().onComplete { as =>
    as
  }

}
