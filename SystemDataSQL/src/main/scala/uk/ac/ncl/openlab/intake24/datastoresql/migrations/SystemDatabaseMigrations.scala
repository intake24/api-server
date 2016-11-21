package uk.ac.ncl.openlab.intake24.datastoresql.migrations

import org.slf4j.Logger
import java.sql.Connection
import anorm.SQL
import uk.ac.ncl.openlab.intake24.sql.migrations.Migration
import uk.ac.ncl.openlab.intake24.sql.migrations.MigrationFailed
import anorm.NamedParameter
import anorm.BatchSql

object SystemDatabaseMigrations {

  val activeMigrations: Seq[Migration] = Seq(
    new Migration {
      val versionFrom = 1l
      val versionTo = 2l

      val description = "Create nutrient_units and nutrient_types"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("""|CREATE TABLE nutrient_units(
               |id integer NOT NULL,
               |description character varying(512) COLLATE pg_catalog."default" NOT NULL,
               |symbol character varying(32) COLLATE pg_catalog."default" NOT NULL,
               |CONSTRAINT nutrient_units_pk PRIMARY KEY (id))""".stripMargin).execute()

        val unitParams = Seq(
          Seq[NamedParameter]('id -> 1, 'description -> "Gram", 'symbol -> "g"),
          Seq[NamedParameter]('id -> 2, 'description -> "Milligram", 'symbol -> "mg"),
          Seq[NamedParameter]('id -> 3, 'description -> "Microgram", 'symbol -> "µg"),
          Seq[NamedParameter]('id -> 4, 'description -> "Kilocalorie", 'symbol -> "kcal"),
          Seq[NamedParameter]('id -> 5, 'description -> "Kilojoule", 'symbol -> "kJ"))

        BatchSql("INSERT INTO nutrient_units VALUES({id},{description},{symbol})", unitParams.head, unitParams.tail: _*).execute()

        SQL("""|CREATE TABLE nutrient_types(id integer NOT NULL,
               |description character varying(512) NOT NULL,
               |unit_id integer NOT NULL,
               |CONSTRAINT nutrient_types_pk PRIMARY KEY (id),
               |CONSTRAINT nutrient_types_nutrient_unit_fk FOREIGN KEY (unit_id) REFERENCES public.nutrient_units (id) ON UPDATE CASCADE ON DELETE CASCADE)""".stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("DROP TABLE nutrient_types").execute()
        SQL("DROP TABLE nutrient_units").execute()

        Right(())
      }
    },

    // 2 -> 3 external

    new Migration {
      val versionFrom = 3l
      val versionTo = 4l

      val description = "Add integer nutrient_id to survey_submission_nutrients"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        Right(())
      }
    })
}
