package uk.ac.ncl.openlab.intake24.foodsql.migrations

import uk.ac.ncl.openlab.intake24.sql.migrations.Migration
import org.slf4j.Logger
import java.sql.Connection
import anorm.SQL

object Migrations {

  val activeMigrations: Seq[Migration] = Seq(
    new Migration {
      val timestamp = 1475852125l
      val description = "Rename as_served_images.url to path and increase size"

      def apply(logger: Logger)(implicit connection: Connection): Unit = {
        SQL("ALTER TABLE as_served_images RENAME COLUMN url TO image_path").execute()
        SQL("ALTER TABLE as_served_images ALTER COLUMN image_path TYPE character varying(1024)").execute()
      }

      def unapply(logger: Logger)(implicit connection: Connection): Unit = {
        SQL("ALTER TABLE as_served_images ALTER COLUMN image_path TYPE character varying(512)").execute()
        SQL("ALTER TABLE as_served_images RENAME COLUMN image_path TO url").execute()
      }
    },
    new Migration {
      val timestamp = 1475852226l
      val description = "Add thumbnail path to as_served_images"

      def apply(logger: Logger)(implicit connection: Connection): Unit = {
        SQL("ALTER TABLE as_served_images ADD COLUMN thumbnail_path character varying(1024)").execute()
        SQL("UPDATE as_served_images SET thumbnail_path=('Thumbnails/' || image_path)").execute()
        SQL("ALTER TABLE as_served_images ALTER COLUMN thumbnail_path SET NOT NULL").execute()
      }

      def unapply(logger: Logger)(implicit connection: Connection): Unit = {
        SQL("ALTER TABLE as_served_images DROP COLUMN thumbnail_path").execute()
      }
    },
    new Migration {
      val timestamp = 1475853279l
      val description = "Create source_images"

      def apply(logger: Logger)(implicit connection: Connection): Unit = {
        SQL("""|CREATE TABLE source_images(
               |  id serial NOT NULL,
               |  path character varying(1024) NOT NULL, 
               |  keywords character varying(512) NOT NULL, 
               |  uploader character varying(256) NOT NULL, 
               |  uploaded_at timestamp without time zone NOT NULL DEFAULT now(),
               |  CONSTRAINT source_images_pk PRIMARY KEY(id)
               |)""".stripMargin).execute()
      }

      def unapply(logger: Logger)(implicit connection: Connection): Unit = {
        SQL("DROP TABLE source_images")
      }
    },
    new Migration {
      val timestamp = 1475853565l
      val description = "Create processed_images"

      def apply(logger: Logger)(implicit connection: Connection): Unit = {
        SQL("""CREATE TABLE processed_images(
                 id serial NOT NULL,
                 path character varying(1024) NOT NULL,
                 source_id integer NOT NULL,
                 purpose integer NOT NULL,
                 created_at timestamp without time zone NOT NULL DEFAULT now(),
                 CONSTRAINT processed_images_pk PRIMARY KEY(id)
               )""".stripMargin).execute()
      }

      def unapply(logger: Logger)(implicit connection: Connection): Unit = {
        SQL("DROP TABLE processed_images)")
      }
    })

}