package uk.ac.ncl.openlab.intake24.foodsql.migrations

import uk.ac.ncl.openlab.intake24.sql.migrations.Migration
import org.slf4j.Logger
import java.sql.Connection

import anorm.{BatchSql, NamedParameter, SQL}
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
    FeedbackDemographicConstraintsFixMigration,
    FeedbackDemographicAlterNutrientRuleType,
    FeedbackDemographicAlterSentimentEnum,
    UnitFixMigration,

    new Migration {

      override val versionFrom: Long = 28
      override val versionTo: Long = 29l
      override val description: String = "Add UAE locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('ar_AE', 'Arabic (UAE)', 'الإمارات العربية المتحدة', 'ar_AE', 'ar', 'ae', 'en_GB')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 29l
      override val versionTo: Long = 30l
      override val description: String = "Add text_direction to locales"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE locales ADD COLUMN text_direction character varying(8) NOT NULL DEFAULT 'ltr'").execute()
        SQL("UPDATE locales SET text_direction='rtl' WHERE id='ar_AE'").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 30l
      override val versionTo: Long = 31l
      override val description: String = "Rename level_of_physical_activity to physical_activity_level"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """
            |UPDATE demographic_group SET physical_activity_level_id=NULL;
            |
            |ALTER TABLE demographic_group DROP CONSTRAINT demographic_group_physical_activity_level_fk;
            |
            |DROP TABLE level_of_physical_activity;
            |
            |CREATE TABLE physical_activity_levels (
            |    id serial PRIMARY KEY,
            |    name character varying(500) NOT NULL,
            |    coefficient numeric(6,3) NOT NULL
            |);
            |
            |ALTER TABLE demographic_group
            |ADD  CONSTRAINT demographic_group_physical_activity_level_id_fk FOREIGN KEY (physical_activity_level_id)
            |REFERENCES physical_activity_levels (id) ON UPDATE CASCADE ON DELETE RESTRICT;
          """.stripMargin).execute()

        Right(())

      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 31l
      override val versionTo: Long = 32l
      override val description: String = "Add physical activity levels"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """
            |INSERT INTO physical_activity_levels (name, coefficient)
            |VALUES ('Sedentary or light activity lifestyle', 1.545),
            |       ('Active or moderately active lifestyle', 1.845),
            |       ('Vigorous or vigorously active lifestyle', 2.2);
          """.stripMargin).execute()

        Right(())

      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    NutrientTableDescription,

    new Migration {

      override val versionFrom: Long = 33l
      override val versionTo: Long = 34l
      override val description: String = "Add New Zealand locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('en_NZ', 'English (New Zealand)', 'English (New Zealand)', 'en_GB', 'en', 'nz', 'en_GB')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    NutrientTableDescriptionLength,

    new Migration {

      override val versionFrom: Long = 35l
      override val versionTo: Long = 36l
      override val description: String = "Delete bad nutrient table record rows"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("DELETE FROM nutrient_table_records WHERE id=''").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    // 36 -> 37 should be set by ImportNutrientTableDescriptions

    new Migration {

      override val versionFrom: Long = 37l
      override val versionTo: Long = 38l
      override val description: String = "Make nutrient_table_records.english_description not nullable"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE nutrient_table_records ALTER COLUMN english_description SET NOT NULL").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 38l
      override val versionTo: Long = 39l
      override val description: String = "Make locale names more descriptive"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("UPDATE locales SET english_name='United Kingdom',local_name='United Kingdom' WHERE id='en_GB'").execute()
        SQL("UPDATE locales SET english_name='Denmark',local_name='Danmark' WHERE id='da_DK'").execute()
        SQL("UPDATE locales SET english_name='Portugal',local_name='Portugal' WHERE id='pt_PT'").execute()
        SQL("UPDATE locales SET english_name='United Arab Emirates',local_name='الإمارات العربية المتحدة' WHERE id='ar_AE'").execute()
        SQL("UPDATE locales SET english_name='New Zealand',local_name='New Zealand' WHERE id='en_NZ'").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 39l
      override val versionTo: Long = 40l
      override val description: String = "Add a UK gluten-free foods locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('en_GB_gf', 'United Kingdom (gluten-free foods)', 'United Kingdom (gluten-free foods)', 'en_GB', 'en', 'gb', 'en_GB')").execute()


        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 40l
      override val versionTo: Long = 41l
      override val description: String = "Fix respondent languages"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("UPDATE locales SET respondent_language_id='pt_PT' WHERE id='pt_PT'").execute()
        SQL("UPDATE locales SET respondent_language_id='da_DK' WHERE id='da_DK'").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 41l
      override val versionTo: Long = 42l
      override val description: String = "Add India (English) locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('en_IN', 'India (English)', 'India (English)', 'en_GB', 'en', 'in', 'en_GB')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 42l
      override val versionTo: Long = 43l
      override val description: String = "Add CO2 emissions nutrient type"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO nutrient_types VALUES(228, 'CO₂ emissions', 1)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 43l
      override val versionTo: Long = 44l
      override val description: String = "Add Australia locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('en_AU', 'Australia', 'Australia', 'en_AU', 'en', 'au', 'en_GB')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 44l
      override val versionTo: Long = 45l
      override val description: String = "Add conversion factor for portion size methods"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE foods_portion_size_methods ADD COLUMN conversion_factor DOUBLE PRECISION NOT NULL DEFAULT 1.0").execute()
        SQL("ALTER TABLE categories_portion_size_methods ADD COLUMN conversion_factor DOUBLE PRECISION NOT NULL DEFAULT 1.0").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 45l
      override val versionTo: Long = 46l
      override val description: String = "Add new nutrient types for Australian FCT"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (229, 'Free sugars', 1)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (230, 'Folic acid', 3)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (231, 'Alpha-linolenic acid', 1)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (232, 'Total long chain omega 3 fatty acids', 2)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (233, 'Total trans fatty acids', 2)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (234, 'Moisture', 1)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (235, 'C20:5w3 Eicosapentaenoic', 2)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (236, 'C22:5w3 Docosapentaenoic', 2)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (237, 'C22:6w3 Docosahexaenoic', 2)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (238, 'Energy, with dietary fibre', 5)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (239, 'Available carbohydrates, with sugar alcohols', 1)").execute()
        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES (240, 'Available carbohydrates, without sugar alcohol', 1)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 46l
      override val versionTo: Long = 47l
      override val description: String = "Add Vitamin A in AU nutrient type"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("INSERT INTO nutrient_units(id, description, symbol) VALUES(6, 'International Units', 'IU')").execute()

        SQL("INSERT INTO nutrient_types(id, description, unit_id) VALUES(241, 'Vitamin A', 6)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 47l
      override val versionTo: Long = 48l
      override val description: String = "Add use in recipes to inheritable attributes"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE categories_attributes ADD COLUMN use_in_recipes INTEGER").execute()
        SQL("ALTER TABLE foods_attributes ADD COLUMN use_in_recipes INTEGER").execute()
        SQL("ALTER TABLE attribute_defaults ADD COLUMN use_in_recipes INTEGER DEFAULT 0").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },


    new Migration {

      override val versionFrom: Long = 48l
      override val versionTo: Long = 49l
      override val description: String = "Create tables for NDNS food group data"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """CREATE TABLE ndns_compound_food_groups(
            | id INTEGER PRIMARY KEY,
            | description CHARACTER VARYING(128) NOT NULL
            |)
          """.stripMargin).execute()

        val groupNames = Seq("Alcoholic beverages",
          "Bread, white",
          "Bread, wholemeal/grain/brown",
          "Cereal",
          "Cereal, white",
          "Cereal, wholemeal/grain/brown",
          "Coffee, Tea, Cocoa or Infusion",
          "Dairy and alternatives",
          "Dairy and alternatives, high fat",
          "Dairy and alternatives, low fat",
          "Discretionary items",
          "Egg",
          "Fats, hard",
          "Fats, liquid",
          "Fats, low fat",
          "Fruit",
          "Fruit, canned",
          "Fruit, cooked",
          "Fruit, dried",
          "Fruit, fresh",
          "Fruit, pureed",
          "Juice",
          "Meats",
          "Meats, processed",
          "Meats, red",
          "Miscellaneous",
          "Mixed",
          "Mixed,  Pulses",
          "Mixed, Fish/ Seafood",
          "Mixed, Vegetable",
          "Nuts and Seeds",
          "Potatoes",
          "Pulses and pulse products",
          "Pulses and pulse products, pureed",
          "Seafood, oily fish",
          "Seafood, organism",
          "Seafood, white fish",
          "Soft drink, low calorie",
          "Soft drink, not low calorie",
          "Starchy vegetables",
          "Supplements",
          "Vegetables",
          "Vegetables, canned",
          "Vegetables, cooked",
          "Vegetables, dried",
          "Vegetables, fresh",
          "Vegetables, pureed",
          "Water",
          "#N/A")


        val params = groupNames.zipWithIndex.map {
          case (desc, index) => Seq[NamedParameter]('id -> (index + 1), 'description -> desc)
        }

        BatchSql("INSERT INTO ndns_compound_food_groups VALUES({id},{description})", params.head, params.tail: _*).execute()

        SQL(
          """CREATE TABLE ndns_compound_food_groups_data (
            |  ndns_food_code INTEGER NOT NULL,
            |  compound_food_group_id INTEGER NOT NULL REFERENCES ndns_compound_food_groups(id),
            |  proportion DOUBLE PRECISION NOT NULL,
            |  CONSTRAINT ndns_compound_food_groups_data_pk PRIMARY KEY (ndns_food_code, compound_food_group_id)
            |)
          """.stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 49l
      override val versionTo: Long = 50l
      override val description: String = "Create five a day feedback table"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """CREATE TABLE five_a_day_feedback(
            |  id SERIAL PRIMARY KEY,
            |  if_less_than INT NOT NULL,
            |  sentiment sentiment_enum NOT NULL,
            |  summary VARCHAR(500) NOT NULL,
            |  feedback VARCHAR(20000) NOT NULL
            |)
          """.stripMargin).execute()

        SQL("""CREATE UNIQUE INDEX five_a_day_feedback_if_less_than_uindex ON five_a_day_feedback (if_less_than)""".stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 50l
      override val versionTo: Long = 51l
      override val description: String = "Drop unused five a day columns"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE five_a_day_feedback DROP COLUMN sentiment").execute()
        SQL("ALTER TABLE five_a_day_feedback DROP COLUMN summary").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 51l
      override val versionTo: Long = 52l
      override val description: String = "Create food group feedback tables"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """create table food_groups_feedback(
            |id serial primary key,
            |name varchar(64) not null,
            |too_high_threshold double precision,
            |too_high_message varchar(512),
            |too_low_threshold double precision,
            |too_low_message varchar(512),
            |tell_me_more_text varchar(20000) NOT NULL)
          """.stripMargin).execute()

        SQL(
          """create table food_groups_feedback_group_ids(
            |food_groups_feedback_id integer references food_groups_feedback(id),
            |food_group_id integer references ndns_compound_food_groups(id),
            |constraint food_groups_feedback_group_ids_pk PRIMARY KEY(food_groups_feedback_id, food_group_id))
          """.stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      override val versionFrom: Long = 52l
      override val versionTo: Long = 53l
      override val description: String = "Clean up five_a_day_feedback table"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("""alter table five_a_day_feedback drop column id""").execute()
        SQL("""alter table five_a_day_feedback drop column if_less_than""").execute()
        SQL("""alter table five_a_day_feedback add column too_low_message VARCHAR(512)""").execute()
        SQL("""alter table five_a_day_feedback rename column feedback to tell_me_more_text""").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }

    },

    new Migration {

      override val versionFrom: Long = 53l
      override val versionTo: Long = 54l
      override val description: String = "Add UK (simplified) locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('en_GB_simple', 'United Kingdom (simplified)', 'United Kingdom (simplified)', 'en_GB', 'en', 'gb', 'en_GB')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {
    override val versionFrom: Long = 54l
    override val versionTo: Long = 55l
    override val description: String = "Drop image_map_id from guide_image_objects"

    override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
      SQL("alter table guide_image_objects drop column image_map_id").execute()

      Right(())
    }

    def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
      ???

    }
  },


    new Migration {
      override val versionFrom: Long = 55l
      override val versionTo: Long = 56l
      override val description: String = "Cascade deletion of image map objects"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE image_map_objects DROP CONSTRAINT image_map_objects_image_map_id_fkey").execute()
        SQL("ALTER TABLE image_map_objects ADD CONSTRAINT image_map_objects_image_map_id_fkey FOREIGN KEY (image_map_id) REFERENCES image_maps (id) ON DELETE CASCADE ON UPDATE CASCADE").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 56l
      override val versionTo: Long = 57l
      override val description: String = "Add New Zealand (v.2) locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('en_NZ_2', 'New Zealand (v.2)', 'New Zealand (v.2)', 'en_GB', 'en', 'nz', 'en_GB')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 57l
      override val versionTo: Long = 58l
      override val description: String = "Add UK (South Asian) locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('en_GB_sa', 'United Kingdom (South Asian)', 'United Kingdom (South Asian)', 'en_GB', 'en', 'gb', 'en_GB')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    }

  )
}
