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

        SQL("""|CREATE TABLE processed_images(
               |  id serial NOT NULL,
               |  path character varying(1024) NOT NULL,
               |  source_id integer NOT NULL,
               |  purpose integer NOT NULL,
               |  created_at timestamp without time zone NOT NULL DEFAULT now(),
               |  CONSTRAINT processed_images_pk PRIMARY KEY(id),
               |  CONSTRAINT processed_images_source_image_fk FOREIGN KEY(source_id) REFERENCES source_images(id) ON UPDATE CASCADE ON DELETE CASCADE               
               |)""".stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("DROP TABLE source_images").execute()
        SQL("DROP TABLE processed_images").execute()

        Right(())
      }
    },
    new Migration {
      val versionFrom = 2l
      val versionTo = 3l

      val description = "Add temporary nullable image_id and thumbnail_image_id columns to as_served_images"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE as_served_images ADD COLUMN image_id integer").execute()
        SQL("ALTER TABLE as_served_images ADD COLUMN thumbnail_image_id integer").execute()
        SQL("ALTER TABLE as_served_images ADD CONSTRAINT image_id_fk FOREIGN KEY(image_id) REFERENCES processed_images(id) ON UPDATE CASCADE ON DELETE CASCADE").execute()
        SQL("ALTER TABLE as_served_images ADD CONSTRAINT thumbnail_image_id_fk FOREIGN KEY(thumbnail_image_id) REFERENCES processed_images(id) ON UPDATE CASCADE ON DELETE CASCADE").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE as_served_images DROP COLUMN image_id").execute()
        SQL("ALTER TABLE as_served_images DROP COLUMN thumbnail_image_id").execute()

        Right(())
      }
    },
    
    // 3 to 4 is complicated, see AsServedToV4
    
    new Migration {
      val versionFrom = 4l
      val versionTo = 5l

      val description = "Apply NON NULL restriction and drop url column from as_served_images"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE as_served_images DROP COLUMN url").execute()
        SQL("ALTER TABLE as_served_images ALTER COLUMN image_id SET NOT NULL").execute()
        SQL("ALTER TABLE as_served_images ALTER COLUMN thumbnail_image_id SET NOT NULL").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE as_served_images ALTER COLUMN image_id DROP NOT NULL").execute()
        SQL("ALTER TABLE as_served_images ALTER COLUMN thumbnail_image_id DROP NOT NULL").execute()        
        SQL("ALTER TABLE as_served_images ADD COLUMN url character varying(512)").execute()

        Right(())
      }
    })
}