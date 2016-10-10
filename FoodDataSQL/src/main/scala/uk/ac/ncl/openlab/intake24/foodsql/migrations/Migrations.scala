package uk.ac.ncl.openlab.intake24.foodsql.migrations

import uk.ac.ncl.openlab.intake24.sql.migrations.Migration
import org.slf4j.Logger
import java.sql.Connection
import anorm.SQL
import uk.ac.ncl.openlab.intake24.sql.migrations.MigrationFailed

object Migrations {

  val activeMigrations: Seq[Migration] = Seq(
    new Migration {
      val versionFrom = 1l
      val versionTo = 2l

      val description = "Create source_images and processed_images"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("""|CREATE TABLE source_images(
               |  id serial NOT NULL,
               |  path character varying(1024) NOT NULL, 
               |  keywords character varying(512) NOT NULL, 
               |  uploader character varying(256) NOT NULL, 
               |  uploaded_at timestamp without time zone NOT NULL DEFAULT now(),
               |  CONSTRAINT source_images_pk PRIMARY KEY(id)
               |)""".stripMargin).execute()

        SQL("""CREATE TABLE processed_images(
                 id serial NOT NULL,
                 path character varying(1024) NOT NULL,
                 source_id integer NOT NULL,
                 purpose integer NOT NULL,
                 created_at timestamp without time zone NOT NULL DEFAULT now(),
                 CONSTRAINT processed_images_pk PRIMARY KEY(id)
               )""".stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("DROP TABLE source_images").execute()
        SQL("DROP TABLE processed_images").execute()

        Right(())
      }
    },
    new AsServedToV3())
}