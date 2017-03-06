package uk.ac.ncl.openlab.intake24.foodsql.migrations

import uk.ac.ncl.openlab.intake24.sql.migrations.Migration
import org.slf4j.Logger
import java.sql.Connection
import anorm.SQL
import uk.ac.ncl.openlab.intake24.sql.migrations.MigrationFailed

object FoodDatabaseMigrations {

  val activeMigrations: Seq[Migration] = Seq(
    new Migration {
      val versionFrom = 1l
      val versionTo = 2l

      val description = "Create source_images and processed_images"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """|CREATE TABLE source_images(
             |  id serial NOT NULL,
             |  path character varying(1024) NOT NULL,
             |  keywords character varying(512) NOT NULL,
             |  uploader character varying(256) NOT NULL,
             |  uploaded_at timestamp without time zone NOT NULL DEFAULT now(),
             |  CONSTRAINT source_images_pk PRIMARY KEY(id)
             |)""".stripMargin).execute()

        SQL(
          """|CREATE TABLE processed_images(
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
    },

    new Migration {
      val versionFrom = 5l
      val versionTo = 6l

      val description = "Convert source_images.keywords to array"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE source_images ALTER COLUMN keywords TYPE character varying(512)[] USING string_to_array(keywords, ' ')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE source_images ALTER COLUMN keywords TYPE character varying(512) USING array_to_string(keywords, ' ')").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 6l
      val versionTo = 7l

      val description = "Add selection screen thumbnail for as served"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE as_served_sets ADD COLUMN selection_image_id integer").execute()
        SQL("CREATE INDEX source_images_path_index ON source_images(path)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE as_served_sets DROP COLUMN selection_image_id").execute()
        SQL("DROP INDEX source_images_path_index").execute()

        Right(())
      }
    },

    // 7 -> 8 see AsServedV8_SelectionImages_Apply 
    new Migration {
      val versionFrom = 8l
      val versionTo = 9l

      val description = "Add constraints for selection screen images"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE as_served_sets ALTER COLUMN selection_image_id SET NOT NULL").execute()
        SQL("ALTER TABLE as_served_sets ADD CONSTRAINT as_served_sets_selection_image_fk FOREIGN KEY(selection_image_id) REFERENCES processed_images(id) ON DELETE CASCADE ON UPDATE CASCADE").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE as_served_sets ALTER COLUMN selection_image_id DROP NOT NULL").execute()
        SQL("ALTER TABLE as_served_sets DROP CONSTRAINT as_served_sets_selection_image_fk").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 9l
      val versionTo = 10l

      val description = "Add thumbnail_path to source_images"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE source_images ADD COLUMN thumbnail_path character varying(1024)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE source_images DROP COLUMN thumbnail_path").execute()

        Right(())
      }
    },

    // 10 -> 11 see V10_SourceImageThumbnails_Apply

    new Migration {
      val versionFrom = 11l
      val versionTo = 12l

      val description = "Add NOT NULL constraint to thumbnail_path in source_images"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE source_images ALTER COLUMN thumbnail_path SET NOT NULL").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE source_images ALTER COLUMN thumbnail_path DROP NOT NULL").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 12l
      val versionTo = 13l

      val description = "Move source_images.keywords to a separate table source_image_keywords"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL(
          """|CREATE TABLE source_image_keywords(
             |  source_image_id integer NOT NULL,
             |  keyword character varying(512) NOT NULL,
             |  CONSTRAINT source_image_keywords_source_image_id_fk FOREIGN KEY(source_image_id) REFERENCES source_images(id) ON DELETE CASCADE ON UPDATE CASCADE)""".stripMargin).execute()

        SQL("CREATE INDEX source_image_keywords_index ON source_image_keywords(keyword varchar_pattern_ops)").execute()

        SQL("CREATE INDEX source_image_keyword_fk_index on source_image_keywords(source_image_id)").execute()

        SQL("INSERT INTO source_image_keywords(source_image_id, keyword) SELECT id, unnest(keywords) FROM source_images").execute()

        SQL("ALTER TABLE source_images DROP COLUMN keywords").execute()

        Right(())
      }

      override def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = Left(MigrationFailed(new Throwable("This migration cannot be unapplied")))
    },

    new Migration {
      val versionFrom = 13l
      val versionTo = 14l

      val description = "Prevent deletion of referenced source images"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE processed_images DROP CONSTRAINT processed_images_source_image_fk").execute()
        SQL("ALTER TABLE processed_images ADD CONSTRAINT processed_images_source_image_fk FOREIGN KEY (source_id) REFERENCES source_images(id) ON UPDATE CASCADE ON DELETE RESTRICT")

        Right(())
      }

      override def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = Left(MigrationFailed(new Throwable("This migration cannot be unapplied")))
    },

    new Migration {
      val versionFrom = 15l
      val versionTo = 16l

      val description = "Rename guide_image_weights to guide_image_objects"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE guide_image_weights RENAME TO guide_image_objects").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE guide_image_objects RENAME TO guide_image_weights").execute()
        Right(())
      }
    },

    new Migration {
      val versionFrom = 16l
      val versionTo = 17l

      val description = "Create image_maps and image_map_objects"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """|CREATE TABLE image_maps(
             |  id character varying(32) NOT NULL PRIMARY KEY,
             |  description character varying(512) NOT NULL,
             |  base_image_id integer NOT NULL REFERENCES processed_images ON UPDATE CASCADE ON DELETE RESTRICT
             |)""".stripMargin).execute()

        SQL(
          """|CREATE TABLE image_map_objects(
             |  id integer NOT NULL,
             |  image_map_id character varying(32) NOT NULL REFERENCES image_maps ON UPDATE CASCADE ON DELETE RESTRICT,
             |  description character varying(512) NOT NULL,
             |  navigation_index integer NOT NULL,
             |  outline_coordinates double precision[] NOT NULL,
             |  overlay_image_id integer NOT NULL REFERENCES processed_images,
             |  CONSTRAINT image_map_objects_pk PRIMARY KEY(id, image_map_id)
             |)""".stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("DROP TABLE image_map_objects").execute()
        SQL("DROP TABLE image_maps")

        Right(())
      }
    },

    new Migration {
      val versionFrom = 17l
      val versionTo = 18l

      val description = "Add temporary nullable columns to guide_images and guide_image_objects"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE guide_images ADD COLUMN image_map_id character varying(32)").execute()
        SQL("ALTER TABLE guide_images ADD COLUMN selection_image_id integer").execute()
        SQL("ALTER TABLE guide_image_objects ADD COLUMN image_map_id character varying(32)").execute()
        SQL("ALTER TABLE guide_image_objects ADD COLUMN image_map_object_id integer").execute()

        SQL("ALTER TABLE guide_images ADD CONSTRAINT guide_image_image_map_fk FOREIGN KEY(image_map_id) REFERENCES image_maps(id) ON UPDATE CASCADE ON DELETE RESTRICT").execute()
        SQL("ALTER TABLE guide_images ADD CONSTRAINT guide_selection_image_id_fk FOREIGN KEY(selection_image_id) REFERENCES processed_images(id) ON UPDATE CASCADE ON DELETE RESTRICT").execute()
        SQL("ALTER TABLE guide_image_objects ADD CONSTRAINT guide_image_object_fk FOREIGN KEY(image_map_object_id, image_map_id) REFERENCES image_map_objects(id, image_map_id) ON UPDATE CASCADE ON DELETE RESTRICT").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE guide_images DROP COLUMN image_map_id").execute()
        SQL("ALTER TABLE guide_images DROP COLUMN selection_image_id").execute()
        SQL("ALTER TABLE guide_image_objects DROP COLUMN image_map_id").execute()
        SQL("ALTER TABLE guide_image_objects DROP COLUMN image_map_object_id").execute()

        Right(())
      }
    },

    // 18 -> 19 FoodV18_Apply
    new Migration {
      val versionFrom = 19l
      val versionTo = 20l

      val description = "Make temporary nullable columns in guide_images and guide_image_objects not nullable"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE guide_images ALTER COLUMN image_map_id SET NOT NULL").execute()
        SQL("ALTER TABLE guide_images ALTER COLUMN selection_image_id SET NOT NULL").execute()
        SQL("ALTER TABLE guide_image_objects ALTER COLUMN image_map_id SET NOT NULL").execute()
        SQL("ALTER TABLE guide_image_objects ALTER COLUMN image_map_object_id SET NOT NULL").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE guide_images ALTER COLUMN image_map_id DROP NOT NULL").execute()
        SQL("ALTER TABLE guide_images ALTER COLUMN selection_image_id DROP NOT NULL").execute()
        SQL("ALTER TABLE guide_image_objects ALTER COLUMN image_map_id DROP NOT NULL").execute()
        SQL("ALTER TABLE guide_image_objects ALTER COLUMN image_map_object_id DROP NOT NULL").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 20l
      val versionTo = 21l

      val description = "Drop base_image_url from guide_images and description from guide_image_objects"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE guide_images DROP COLUMN base_image_url").execute()
        SQL("ALTER TABLE guide_image_objects DROP COLUMN description").execute()
        SQL("ALTER TABLE guide_image_objects DROP COLUMN object_id").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    FeedbackDemographicScaleMigration,
    FeedbackDemographicScaleRangeNotEmpty,
    FeedbackDemographicNutrientDescriptionsMigration,
    FeedbackDemographicConstraintsFixMigration

  )
}
