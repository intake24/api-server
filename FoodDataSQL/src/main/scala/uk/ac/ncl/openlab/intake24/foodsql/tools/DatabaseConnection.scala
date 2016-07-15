package uk.ac.ncl.openlab.intake24.foodsql.tools

import java.sql.DriverManager
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig
import java.util.Properties
import java.io.PrintWriter
import org.rogach.scallop.ScallopConf

case class DatabaseOptions(arguments: Seq[String]) extends ScallopConf(arguments) {
  val pgHost = opt[String](required = true, noshort = true)
  val pgDatabase = opt[String](required = true, noshort = true)
  val pgUser = opt[String](required = true, noshort = true)
  val pgPassword = opt[String](noshort = true)
  val pgUseSsl = opt[Boolean](noshort = true)
}

trait DatabaseConnection {
  
  def getDataSource(arguments: Seq[String]) = {
    
    val options = DatabaseOptions(arguments)

    DriverManager.registerDriver(new org.postgresql.Driver)

    val dbConnectionProps = new Properties();
    dbConnectionProps.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
    dbConnectionProps.setProperty("dataSource.user", options.pgUser())
    dbConnectionProps.setProperty("dataSource.databaseName", options.pgDatabase())
    dbConnectionProps.setProperty("dataSource.serverName", options.pgHost())
    dbConnectionProps.put("dataSource.logWriter", new PrintWriter(System.out))

    options.pgPassword.foreach(pw => dbConnectionProps.setProperty("dataSource.password", pw))
    options.pgUseSsl.foreach(ssl => dbConnectionProps.setProperty("dataSource.ssl", ssl.toString()))

    new HikariDataSource(new HikariConfig(dbConnectionProps))
  }
}
