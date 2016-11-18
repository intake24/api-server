package uk.ac.ncl.openlab.intake24.sql.tools.food

import org.slf4j.LoggerFactory
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.NutrientTableRecord
import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableParser
import uk.ac.ncl.openlab.intake24.nutrientsndns.LegacyNutrientTables
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.foodsql.images.ImageDatabaseServiceSqlImpl
import uk.ac.ncl.openlab.intake24.sql.migrations.MigrationsImpl
import uk.ac.ncl.openlab.intake24.sql.migrations.MigrationFailed
import uk.ac.ncl.openlab.intake24.sql.migrations.SimpleDatabaseError

object Migrate extends App with WarningMessage with DatabaseConnection {

  val logger = LoggerFactory.getLogger(getClass)

  trait Options extends ScallopConf {
    version("Intake24 food database migration 2.2.0-SNAPSHOT")
  }

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.verify()

  displayWarningMessage("Warning: this will change the database format. Make sure your client code is compatible.")

  val dataSource = getDataSource(options)

  val migrations = new MigrationsImpl(dataSource)

  migrations.applyMigrations(uk.ac.ncl.openlab.intake24.foodsql.migrations.Migrations.activeMigrations) match {
    case Left(MigrationFailed(e)) => logger.error("Migration failed", e)
    case Left(SimpleDatabaseError(e)) => logger.error("Database error", e)
    case Right(()) => {}
  }
}
