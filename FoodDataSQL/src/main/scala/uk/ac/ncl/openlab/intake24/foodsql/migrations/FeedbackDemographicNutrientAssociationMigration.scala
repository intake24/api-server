package uk.ac.ncl.openlab.intake24.foodsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by Tim Osadchiy on 01/03/2017.
  */
object FeedbackDemographicNutrientAssociationMigration extends Migration{

  val versionFrom = 23l
  val versionTo = 24l

  val description = "Create association table for nutrients and demographics, containing the description of rules"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |CREATE TABLE nutrient_demographic_association (
        |     id serial PRIMARY KEY,
        |     nutrient_type_id integer NOT NULL,
        |     demographic_group_id integer UNIQUE NOT NULL,
        |     description character varying(1000),
        |     CONSTRAINT nutrient_types_id_fk FOREIGN KEY (nutrient_type_id) REFERENCES nutrient_types(id),
        |     CONSTRAINT demographic_group_fk FOREIGN KEY (demographic_group_id) REFERENCES demographic_group(id)
        |);
        |
        |INSERT INTO nutrient_demographic_association (nutrient_type_id, demographic_group_id)
        |SELECT nutrient_type_id, id
        |FROM demographic_group;
        |
        |ALTER TABLE demographic_group DROP COLUMN nutrient_type_id;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |ALTER TABLE demographic_group ADD COLUMN nutrient_type_id integer;
        |
        |UPDATE demographic_group
        |SET nutrient_type_id = nda.nutrient_type_id
        |FROM nutrient_demographic_association AS nda
        |WHERE demographic_group.id = nda.demographic_group_id;
        |
        |ALTER TABLE demographic_group ALTER COLUMN nutrient_type_id SET NOT NULL;
        |
        |DROP TABLE nutrient_demographic_association;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}