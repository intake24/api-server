package uk.ac.ncl.openlab.intake24.systemsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by Tim Osadchiy on 01/03/2017.
  */
object AlterWeightTargetEnum extends Migration {

  val versionFrom = 46l
  val versionTo = 47l

  val description = "Alter weight target enum to fix loOse typo"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |ALTER TYPE weight_target_enum RENAME TO old_weight_target_enum;
        |
        |CREATE TYPE weight_target_enum
        |AS enum('keep_weight', 'loose_weight', 'lose_weight', 'gain_weight');
        |
        |ALTER TABLE user_physical_data ALTER COLUMN weight_target TYPE weight_target_enum
        |USING weight_target::text::weight_target_enum;
        |
        |DROP TYPE old_weight_target_enum;
        |
        |UPDATE user_physical_data AS upd SET weight_target = 'lose_weight'
        |WHERE upd.weight_target = 'loose_weight';
        |
        |ALTER TYPE weight_target_enum RENAME TO old_weight_target_enum;
        |
        |CREATE TYPE weight_target_enum
        |AS enum('keep_weight', 'lose_weight', 'gain_weight');
        |
        |ALTER TABLE user_physical_data ALTER COLUMN weight_target TYPE weight_target_enum
        |USING weight_target::text::weight_target_enum;
        |
        |DROP TYPE old_weight_target_enum;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = ???

}