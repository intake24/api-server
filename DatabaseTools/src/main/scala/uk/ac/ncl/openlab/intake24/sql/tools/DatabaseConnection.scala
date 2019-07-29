package uk.ac.ncl.openlab.intake24.sql.tools

import java.io.PrintWriter
import java.sql.DriverManager
import java.util.Properties
import javax.sql.DataSource

import anorm.{SQL, SqlParser}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.rogach.scallop.ScallopConf
import org.slf4j.Logger

import scala.language.reflectiveCalls

trait DatabaseOptions extends ScallopConf {
  val pgHost = opt[String](required = true, noshort = true)
  val pgDatabase = opt[String](required = true, noshort = true)
  val pgUser = opt[String](required = true, noshort = true)
  val pgPassword = opt[String](noshort = true)
  val pgUseSsl = opt[Boolean](noshort = true)

  def databaseConfig = DatabaseConfiguration(pgHost(), pgUseSsl(), pgDatabase(), pgUser(), pgPassword.toOption)
}

trait DatabaseConfigurationOptions extends ScallopConf {

  val dbConfigDir = opt[String](required = true)
}

case class DatabaseConfiguration(host: String, useSsl: Boolean, database: String, user: String, password: Option[String])

trait DatabaseConnection {

  def chooseDatabaseConfiguration(options: DatabaseConfigurationOptions) = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())

  def getDataSource(options: DatabaseOptions): DataSource = getDataSource(options.databaseConfig)

  def getDataSource(config: DatabaseConfiguration): DataSource = {

    DriverManager.registerDriver(new org.postgresql.Driver)

    val dbConnectionProps = new Properties();
    dbConnectionProps.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
    dbConnectionProps.setProperty("dataSource.user", config.user)
    dbConnectionProps.setProperty("dataSource.databaseName", config.database)
    dbConnectionProps.setProperty("dataSource.serverName", config.host)
    dbConnectionProps.put("dataSource.logWriter", new PrintWriter(System.out))
    dbConnectionProps.put("maximumPoolSize", "1")

    config.password.foreach(pw => dbConnectionProps.setProperty("dataSource.password", pw))

    if (config.useSsl) {
      dbConnectionProps.setProperty("dataSource.ssl", "true")
    }

    new HikariDataSource(new HikariConfig(dbConnectionProps))
  }

  def dropAllTables(implicit connection: java.sql.Connection, logger: Logger) = {

    val dropTableStatements =
      SQL("""SELECT 'DROP TABLE IF EXISTS ' || tablename || ' CASCADE;' AS query FROM pg_tables WHERE schemaname='public'""")
        .executeQuery()
        .as(SqlParser.str("query").*)

    val dropSequenceStatements =
      SQL("""SELECT 'DROP SEQUENCE IF EXISTS ' || relname || ' CASCADE;' AS query FROM pg_class WHERE relkind = 'S'""")
        .executeQuery()
        .as(SqlParser.str("query").*)

    val clearDbStatements = dropTableStatements ++ dropSequenceStatements

    clearDbStatements.foreach {
      statement =>
        logger.debug(statement)
        SQL(statement).execute()
    }
  }
}

object DatabaseConnection extends DatabaseConnection
