package uk.ac.ncl.openlab.intake24.sql.migrations

import java.util.UUID
import org.slf4j.Logger

abstract class Migration {
  val timestamp: Long
  val description: String
  
  def apply(logger: Logger)(implicit connection: java.sql.Connection): Unit
  def unapply(logger: Logger)(implicit connection: java.sql.Connection): Unit
}
