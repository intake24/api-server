package uk.ac.ncl.openlab.intake24.datastoresql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by b0915218 on 08/02/2017.
  */
object FeedbackDemographicScaleMigration extends Migration {

  val versionFrom = 11l
  val versionTo = 12l

  val description = "Create demographic_scale and demographic_scale_sector for giving participants feedback on their survey"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """|CREATE EXTENSION btree_gist;
         |
         |CREATE TYPE sex_enum AS ENUM ('f', 'm');
         |
         |CREATE TYPE sentiment_enum AS ENUM ('highly_negative', 'negative', 'warning',
         |                                    'neutral', 'positive', 'highly_positive');
         |
         |CREATE TABLE level_of_physical_activity (
         |    id serial PRIMARY KEY,
         |    name character varying(500) NOT NULL
         |);
         |
         |CREATE TABLE demographic_scale (
         |    id serial PRIMARY KEY,
         |    age int4range,
         |    height numrange,
         |    weight numrange,
         |    sex sex_enum,
         |    physical_activity_level_id integer NOT NULL,
         |    nutrient_type_id integer NOT NULL,
         |    CONSTRAINT demographic_scale_nutrient_type_fk FOREIGN KEY(physical_activity_level_id)
         |        REFERENCES level_of_physical_activity(id),
         |    CONSTRAINT demographic_scale_physical_activity_level_fk FOREIGN KEY(nutrient_type_id)
         |        REFERENCES nutrient_types(id),
         |    CONSTRAINT factor_scale_age_positive CHECK (lower(age) >= 0),
         |    CONSTRAINT factor_scale_height_positive CHECK (lower(height) >= 0),
         |    CONSTRAINT factor_scale_weight_positive CHECK (lower(weight) >= 0),
         |    CONSTRAINT factor_scale_sex_check CHECK (sex IN ('m', 'f'))
         |);
         |
         |COMMENT ON TABLE demographic_scale IS 'Table for storing demographic rule groups of nutrient ranges for giving participants feedback on their survey';
         |
         |CREATE TABLE demographic_scale_sector (
         |    id serial PRIMARY KEY,
         |    demographic_scale_id integer NOT NULL,
         |    name character varying(500) NOT NULL,
         |    description character varying(50000),
         |    sentiment sentiment_enum NOT NULL DEFAULT 'neutral',
         |    range numrange NOT NULL,
         |    CONSTRAINT demographic_scale_sector_demographic_scale_fk FOREIGN KEY(demographic_scale_id)
         |        REFERENCES demographic_scale(id),
         |    CONSTRAINT demographic_scale_sector_no_overlap EXCLUDE USING gist (demographic_scale_id WITH =, range WITH &&)
         |);
         |
         |COMMENT ON TABLE demographic_scale_sector IS 'Table for storing demographic rules of nutrient ranges for giving participants feedback on their survey';
         |
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())
  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """|DROP EXTENSION btree_gist;
         |DROP TABLE demographic_scale_sector;
         |DROP TABLE demographic_scale;
         |DROP TABLE level_of_physical_activity;
         |DROP TYPE sentiment_enum;
         |DROP TYPE sex_enum;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}
