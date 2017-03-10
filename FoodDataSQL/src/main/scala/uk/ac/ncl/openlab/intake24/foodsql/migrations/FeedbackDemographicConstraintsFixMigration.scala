package uk.ac.ncl.openlab.intake24.foodsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by Tim Osadchiy on 01/03/2017.
  */
object FeedbackDemographicConstraintsFixMigration extends Migration {

  val versionFrom = 24l
  val versionTo = 25l

  val description = "Fixes demographic_group constraints"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |ALTER TABLE demographic_group
        |DROP CONSTRAINT demographic_group_physical_activity_level_fk;
        |
        |ALTER TABLE demographic_group
        |DROP CONSTRAINT demographic_group_nutrient_type_fk;
        |
        |ALTER TABLE demographic_group
        |ADD CONSTRAINT demographic_group_nutrient_type_fk
        |FOREIGN KEY(nutrient_type_id)
        |REFERENCES nutrient_types(id);
        |
        |ALTER TABLE demographic_group
        |ADD CONSTRAINT demographic_group_physical_activity_level_fk
        |FOREIGN KEY(physical_activity_level_id)
        |REFERENCES level_of_physical_activity(id);
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |ALTER TABLE demographic_group
        |DROP CONSTRAINT demographic_group_nutrient_type_fk;
        |
        |ALTER TABLE demographic_group
        |DROP CONSTRAINT demographic_group_physical_activity_level_fk;
        |
        |ALTER TABLE demographic_group
        |ADD CONSTRAINT demographic_group_nutrient_type_fk
        |FOREIGN KEY(physical_activity_level_id)
        |REFERENCES level_of_physical_activity(id);
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}