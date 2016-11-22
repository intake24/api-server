package uk.ac.ncl.openlab.intake24.sql.tools.system

import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.sql.migrations.{MigrationFailed, MigrationsImpl, SimpleDatabaseError}
import uk.ac.ncl.openlab.intake24.sql.tools._

object MigrateSystemDatabase extends App with DatabaseConnection with WarningMessage {

  val logger = LoggerFactory.getLogger(getClass)

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())

  displayWarningMessage(s"Warning: this will change the format of ${databaseConfig.host}/${databaseConfig.database}. Are you sure that is what you want?")

  val dataSource = getDataSource(databaseConfig)

  val migrations = new MigrationsImpl(dataSource)

  migrations.applyMigrations(uk.ac.ncl.openlab.intake24.datastoresql.migrations.SystemDatabaseMigrations.activeMigrations) match {
    case Left(MigrationFailed(e)) => throw e
    case Left(SimpleDatabaseError(e)) => throw e
    case Right(()) => {}
  }
}
