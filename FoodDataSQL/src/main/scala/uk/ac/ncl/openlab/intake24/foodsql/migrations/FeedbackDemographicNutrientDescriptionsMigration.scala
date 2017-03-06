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
        |CREATE TYPE nutrient_rule_type_enum
        |AS ENUM ('percentage_of_energy', 'energy_divided_by_bmr', 'range');
        |
        |CREATE TABLE nutrient_type_in_kcal (
        |   id serial PRIMARY KEY,
        |   nutrient_type_id integer NOT NULL UNIQUE,
        |   kcal_per_unit numeric(10,3) NOT NULL,
        |   CONSTRAINT nutrient_type_fk FOREIGN KEY(nutrient_type_id)
        |   REFERENCES nutrient_types(id)
        |);
        |
        |ALTER TABLE demographic_group
        |ADD COLUMN nutrient_rule_type nutrient_rule_type_enum DEFAULT 'range';
        |
        |UPDATE demographic_group SET nutrient_rule_type = 'range';
        |
        |ALTER TABLE demographic_group
        |ALTER COLUMN nutrient_rule_type SET NOT NULL;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |ALTER TABLE demographic_group DROP COLUMN nutrient_rule_type;
        |DROP TABLE nutrient_type_in_kcal;
        |DROP TYPE nutrient_rule_type_enum;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}