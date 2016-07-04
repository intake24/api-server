package uk.ac.ncl.openlab.intake24.foodsql.scripts

import java.io.PrintWriter
import java.util.Properties

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

trait DatabaseScript {

  def getDataSource(pgHost: String, pgUser: String, pgPassword: Option[String], pgUseSsl: Boolean, pgDatabase: String) = {
    val dbConnectionProps = new Properties()
    dbConnectionProps.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
    dbConnectionProps.setProperty("dataSource.user", pgUser)
    dbConnectionProps.setProperty("dataSource.databaseName", pgDatabase)
    dbConnectionProps.setProperty("dataSource.serverName", pgHost)
    dbConnectionProps.put("dataSource.logWriter", new PrintWriter(System.out))
    dbConnectionProps.setProperty("dataSource.ssl", pgUseSsl.toString())

    pgPassword.foreach(pw => dbConnectionProps.setProperty("dataSource.password", pw))

    new HikariDataSource(new HikariConfig(dbConnectionProps))
  }

  def getLocalDataSource(pgDatabase: String) =
    getDataSource("localhost", "postgres", None, false, pgDatabase)
}