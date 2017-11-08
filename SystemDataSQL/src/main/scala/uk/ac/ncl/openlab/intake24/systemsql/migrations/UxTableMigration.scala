package uk.ac.ncl.openlab.intake24.systemsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by Tim Osadchiy on 08/11/2017.
  */
object UxTableMigration extends Migration {
  val versionFrom = 64l
  val versionTo = 65l

  val description = "Create ux events table"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    SQL(
      """|CREATE TABLE ux_events (
         |  id SERIAL PRIMARY KEY,
         |  event_categories VARCHAR(500)[] NOT NULL,
         |  event_type VARCHAR(500) NOT NULL,
         |  data JSON NOT NULL,
         |  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
         |)""".stripMargin).execute()

    Right(())
  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    SQL("DROP TABLE ux_events").execute()

    Right(())
  }
}
