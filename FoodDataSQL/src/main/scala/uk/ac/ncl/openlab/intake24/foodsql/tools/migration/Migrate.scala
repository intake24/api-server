package uk.ac.ncl.openlab.intake24.foodsql.tools.migration

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseConnection
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.foodsql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.foodsql.migrations.MigrationsImpl

trait Options extends ScallopConf {
  version("Intake24 SQL database migration tool v16.9")

  val xmlPath = opt[String](required = true, noshort = true)
}

object Migrate extends App with WarningMessage with DatabaseConnection {

  val logger = LoggerFactory.getLogger(getClass)

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  displayWarningMessage("This is a dangerous operation that might result in data loss! Make sure to have backed up the data.")

  val dataSource = getDataSource(options)

  val service = new MigrationsImpl(dataSource)

  service.applyNewMigrations(Seq())

}
