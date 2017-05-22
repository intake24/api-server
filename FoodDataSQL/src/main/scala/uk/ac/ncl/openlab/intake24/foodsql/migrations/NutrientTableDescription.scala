package uk.ac.ncl.openlab.intake24.foodsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by b0915218 on 22/05/2017.
  */
object NutrientTableDescription extends Migration {

  val versionFrom = 32l
  val versionTo = 33l

  val description = "Add description columns to nutrient_table_records"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """|ALTER TABLE nutrient_table_records
         |ADD COLUMN english_description character varying(128);
         |
         |ALTER TABLE nutrient_table_records
         |ADD COLUMN local_description character varying(128);
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())
  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """|ALTER TABLE nutrient_table_records
         |DROP COLUMN english_description;
         |
         |ALTER TABLE nutrient_table_records
         |DROP COLUMN local_description;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}
