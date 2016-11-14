package uk.ac.ncl.openlab.intake24.sql.migrations

import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import javax.sql.DataSource

abstract class Migration {
  val versionFrom: Long
  val versionTo: Long
  val description: String
  
  def apply(logger: Logger)(implicit connection: java.sql.Connection): Either[MigrationFailed, Unit]
  def unapply(logger: Logger)(implicit connection: java.sql.Connection): Either[MigrationFailed, Unit]
}
