package uk.ac.ncl.openlab.intake24.foodsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by Tim Osadchiy on 01/03/2017.
  */
object FeedbackDemographicAlterNutrientRuleType extends Migration {

  val versionFrom = 25l
  val versionTo = 26l

  val description = "Add per_unit_of_weight to nutrient rule types"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |ALTER TYPE nutrient_rule_type_enum RENAME TO old_nutrient_rule_type_enum;
        |
        |ALTER TABLE demographic_group ALTER COLUMN nutrient_rule_type DROP DEFAULT;
        |
        |CREATE TYPE nutrient_rule_type_enum AS enum('percentage_of_energy', 'energy_divided_by_bmr', 'per_unit_of_weight', 'range');
        |
        |ALTER TABLE demographic_group ALTER COLUMN nutrient_rule_type TYPE nutrient_rule_type_enum
        |USING nutrient_rule_type::text::nutrient_rule_type_enum;
        |
        |ALTER TABLE demographic_group ALTER COLUMN nutrient_rule_type SET DEFAULT 'range';
        |
        |DROP TYPE old_nutrient_rule_type_enum;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |ALTER TYPE nutrient_rule_type_enum RENAME TO old_nutrient_rule_type_enum;
        |
        |ALTER TABLE demographic_group ALTER COLUMN nutrient_rule_type DROP DEFAULT;
        |
        |CREATE TYPE nutrient_rule_type_enum AS enum('percentage_of_energy', 'energy_divided_by_bmr', 'range');
        |
        |ALTER TABLE demographic_group ALTER COLUMN nutrient_rule_type TYPE nutrient_rule_type_enum
        |USING nutrient_rule_type::text::nutrient_rule_type_enum;
        |
        |ALTER TABLE demographic_group ALTER COLUMN nutrient_rule_type SET DEFAULT 'range';
        |
        |DROP TYPE old_nutrient_rule_type_enum;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}