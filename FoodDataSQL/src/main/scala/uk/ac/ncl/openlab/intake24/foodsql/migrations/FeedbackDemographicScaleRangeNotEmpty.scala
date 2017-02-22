package uk.ac.ncl.openlab.intake24.foodsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by b0915218 on 08/02/2017.
  */
object FeedbackDemographicScaleRangeNotEmpty extends Migration {

  val versionFrom = 22l
  val versionTo = 23l

  val description = "demographic_group_scale_sector range not empty"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """|DELETE FROM demographic_group_scale_sector
         |WHERE isempty(range);
         |
         |ALTER TABLE demographic_group_scale_sector
         |ADD CONSTRAINT demographic_group_scale_sector_range_not_empty
         |CHECK (NOT isempty(range));
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())
  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """|ALTER TABLE demographic_group_scale_sector
         |DROP CONSTRAINT demographic_group_scale_sector_range_not_empty;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}
