package uk.ac.ncl.openlab.intake24.foodsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by b0915218 on 22/05/2017.
  */
object NutrientTableDescriptionLength extends Migration {

  val versionFrom = 34l
  val versionTo = 35l

  val description = "Add description columns to nutrient_table_records"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """|ALTER TABLE nutrient_table_records
         |ALTER COLUMN english_description TYPE character varying(512);
         |
         |ALTER TABLE nutrient_table_records
         |ALTER COLUMN local_description TYPE character varying(512);
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())
  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """|ALTER TABLE nutrient_table_records
         |ALTER COLUMN english_description TYPE character varying(128);
         |
         |ALTER TABLE nutrient_table_records
         |ALTER COLUMN local_description TYPE character varying(128);
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}
