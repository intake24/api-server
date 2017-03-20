package uk.ac.ncl.openlab.intake24.foodsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by Tim Osadchiy on 01/03/2017.
  */
object UnitFixMigration extends Migration {

  val versionFrom = 27l
  val versionTo = 28l

  val description = "Fix units for nutrient types"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |UPDATE nutrient_types SET unit_id = '2'
        |WHERE id IN ('59', '123', '124', '125', '126', '127', '129', '130', '131', '132',
        |             '138', '139', '140', '141', '142', '143', '144', '145', '146', '147',
        |             '148', '151', '154', '155', '156', '158', '160', '161', '164', '176',
        |             '177', '178', '179', '181', '182', '196', '197', '199', '200', '201',
        |             '202', '203', '204', '205', '206', '207');
        |
        |UPDATE nutrient_types SET unit_id = '3'
        |WHERE id IN ('114', '115', '116', '117', '118', '119', '120', '121', '122', '133',
        |             '134', '135', '136', '137', '149', '150', '152', '162', '163', '174',
        |             '175', '180', '183', '184', '185', '186', '187', '188', '189', '190',
        |             '191');
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |UPDATE nutrient_types SET unit_id = '1'
        |WHERE id IN ('59', '114', '115', '116', '117', '118',
        |             '119', '120', '121', '122', '123', '124',
        |             '125', '126', '127', '129', '130', '131',
        |             '132', '133', '134', '135', '136', '137',
        |             '138', '139', '140', '141', '142', '143',
        |             '144', '145', '146', '147', '148', '149',
        |             '150', '151', '152', '154', '155', '156',
        |             '158', '160', '161', '162', '163', '164',
        |             '174', '175', '176', '177', '178', '179',
        |             '180', '181', '182', '183', '184', '185',
        |             '186', '187', '188', '189', '190', '191',
        |             '196', '197', '199', '200', '201', '202',
        |             '203', '204', '205', '206', '207');
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}