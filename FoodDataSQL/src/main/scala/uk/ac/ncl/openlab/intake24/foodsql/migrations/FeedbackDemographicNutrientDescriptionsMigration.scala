package uk.ac.ncl.openlab.intake24.foodsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by Tim Osadchiy on 01/03/2017.
  */
object FeedbackDemographicNutrientDescriptionsMigration extends Migration{

  val versionFrom = 23l
  val versionTo = 24l

  val description = "Create table with nutrients demographics rules descriptions"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |CREATE TABLE nutrient_demographic_rules_descriptions (
        |     id serial PRIMARY KEY,
        |     nutrient_type_id integer NOT NULL UNIQUE,
        |     description character varying(1000),
        |     CONSTRAINT nutrient_types_id_fk FOREIGN KEY (nutrient_type_id) REFERENCES nutrient_types(id)
        |);
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |DROP TABLE nutrient_demographic_rules_descriptions;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}