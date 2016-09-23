package uk.ac.ncl.openlab.intake24.foodsql.migrations

import org.joda.time.DateTime
import java.util.UUID
import org.slf4j.Logger

trait Migration {
  def id: UUID
  
  def upgrade(logger: Logger)(implicit connection: java.sql.Connection): Unit
  def downgrade(logger: Logger)(implicit connection: java.sql.Connection): Unit
}
