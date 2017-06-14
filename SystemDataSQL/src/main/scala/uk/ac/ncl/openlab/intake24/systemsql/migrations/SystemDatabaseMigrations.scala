package uk.ac.ncl.openlab.intake24.systemsql.migrations

import java.sql.Connection

import anorm.{BatchSql, NamedParameter, SQL, SqlParser}
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.URLAuthTokenUtils
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

import anorm.~

object SystemDatabaseMigrations {

  val activeMigrations: Seq[Migration] = Seq(
    new Migration {
      val versionFrom = 1l
      val versionTo = 2l

      val description = "Create nutrient_units and nutrient_types"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """|CREATE TABLE nutrient_units(
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

        SQL(
          """|CREATE TABLE nutrient_types(id integer NOT NULL,
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

    // 2 -> 3 see SystemV2_CreateMasterNutrientList

    new Migration {
      val versionFrom = 3l
      val versionTo = 4l

      val mapping = Seq(
        Seq[NamedParameter]('legacy_name -> "protein", 'type_id -> 11l),
        Seq[NamedParameter]('legacy_name -> "fat", 'type_id -> 49l),
        Seq[NamedParameter]('legacy_name -> "carbohydrate", 'type_id -> 13l),
        Seq[NamedParameter]('legacy_name -> "energy_kcal", 'type_id -> 1l),
        Seq[NamedParameter]('legacy_name -> "energy_kj", 'type_id -> 2l),
        Seq[NamedParameter]('legacy_name -> "alcohol", 'type_id -> 20l),
        Seq[NamedParameter]('legacy_name -> "total_sugars", 'type_id -> 22l),
        Seq[NamedParameter]('legacy_name -> "nmes", 'type_id -> 23l),
        Seq[NamedParameter]('legacy_name -> "satd_fa", 'type_id -> 50l),
        Seq[NamedParameter]('legacy_name -> "cholesterol", 'type_id -> 59l),
        Seq[NamedParameter]('legacy_name -> "vitamin_a", 'type_id -> 120l),
        Seq[NamedParameter]('legacy_name -> "vitamin_d", 'type_id -> 122l),
        Seq[NamedParameter]('legacy_name -> "vitamin_c", 'type_id -> 129l),
        Seq[NamedParameter]('legacy_name -> "vitamin_e", 'type_id -> 130l),
        Seq[NamedParameter]('legacy_name -> "folate", 'type_id -> 134l),
        Seq[NamedParameter]('legacy_name -> "sodium", 'type_id -> 138l),
        Seq[NamedParameter]('legacy_name -> "iron", 'type_id -> 143l),
        Seq[NamedParameter]('legacy_name -> "zinc", 'type_id -> 147l),
        Seq[NamedParameter]('legacy_name -> "selenium", 'type_id -> 152l),
        Seq[NamedParameter]('legacy_name -> "dietary_fiber", 'type_id -> 17l),
        Seq[NamedParameter]('legacy_name -> "total_monosac", 'type_id -> 35l),
        Seq[NamedParameter]('legacy_name -> "organic_acids", 'type_id -> 47l),
        Seq[NamedParameter]('legacy_name -> "pufa", 'type_id -> 52l),
        Seq[NamedParameter]('legacy_name -> "nacl", 'type_id -> 154l),
        Seq[NamedParameter]('legacy_name -> "ash", 'type_id -> 157l),
        Seq[NamedParameter]('legacy_name -> "calcium", 'type_id -> 140l)
      )

      val description = "Add new integer nutrient_id to survey_submission_nutrients"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE survey_submission_nutrients ADD COLUMN nutrient_type_id integer").execute()
        SQL("ALTER TABLE survey_submission_nutrients ADD CONSTRAINT ssn_nutrient_type_id_fk FOREIGN KEY(nutrient_type_id) REFERENCES nutrient_types(id)").execute()

        BatchSql("UPDATE survey_submission_nutrients SET nutrient_type_id={type_id} WHERE name={legacy_name}", mapping.head, mapping.tail: _*).execute()

        val missingIds = SQL("SELECT name FROM survey_submission_nutrients WHERE nutrient_type_id IS NULL").executeQuery().as(SqlParser.str("name").*)

        if (missingIds.nonEmpty) {
          Left(MigrationFailed(new RuntimeException("Missing nutrient ids for legacy names: " + missingIds.distinct.mkString(", "))))
        } else {
          SQL("ALTER TABLE survey_submission_nutrients ALTER COLUMN nutrient_type_id SET NOT NULL").execute()
          SQL("ALTER TABLE survey_submission_nutrients DROP COLUMN name").execute()
          Right(())
        }
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE survey_submission_nutrients ADD COLUMN name character varying(64)").execute()

        BatchSql("UPDATE survey_submission_nutrients SET name={legacy_name} WHERE nutrient_type_id={type_id}", mapping.head, mapping.tail: _*).execute()

        SQL("ALTER TABLE survey_submission_nutrients ALTER COLUMN name SET NOT NULL").execute()

        SQL("ALTER TABLE survey_submission_nutrients DROP COLUMN nutrient_type_id").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 4l
      val versionTo = 5l

      val description = "Create locales"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """|CREATE TABLE locales
             |(
             |    id character varying(16) NOT NULL,
             |    english_name character varying(64) NOT NULL,
             |    local_name character varying(64) NOT NULL,
             |    respondent_language_id character varying(16) NOT NULL,
             |    admin_language_id character varying(16) NOT NULL,
             |    country_flag_code character varying(16) NOT NULL,
             |    prototype_locale_id character varying(16),
             |    CONSTRAINT locales_pk PRIMARY KEY (id),
             |    CONSTRAINT locales_prototype_locale_id_fk FOREIGN KEY (prototype_locale_id)
             |        REFERENCES locales (id)
             |        ON UPDATE CASCADE
             |        ON DELETE CASCADE
             |)""".stripMargin).execute()

        val localeParams = Seq(
          Seq[NamedParameter]('id -> "en_GB", 'english_name -> "English (United Kingdom)", 'local_name -> "English (United Kingdom)", 'respondent_language_id -> "en_GB", 'admin_language_id -> "en", 'country_flag_code -> "gb", 'prototype_locale_id -> None),
          Seq[NamedParameter]('id -> "da_DK", 'english_name -> "Danish (Denmark)", 'local_name -> "Dansk (Danmark)", 'respondent_language_id -> "da", 'admin_language_id -> "da", 'country_flag_code -> "dk", 'prototype_locale_id -> Some("en_GB")),
          Seq[NamedParameter]('id -> "pt_PT", 'english_name -> "Portuguese (Portugal)", 'local_name -> "Português (Portugal)", 'respondent_language_id -> "pt", 'admin_language_id -> "pt", 'country_flag_code -> "pt", 'prototype_locale_id -> Some("en_GB")),
          Seq[NamedParameter]('id -> "en_NZ", 'english_name -> "English (New Zealand)", 'local_name -> "English (New Zealand)", 'respondent_language_id -> "en_NZ", 'admin_language_id -> "en", 'country_flag_code -> "nz", 'prototype_locale_id -> Some("en_GB"))
        )

        BatchSql("INSERT INTO locales VALUES({id},{english_name},{local_name},{respondent_language_id},{admin_language_id},{country_flag_code},{prototype_locale_id})", localeParams.head, localeParams.tail: _*).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("DROP TABLE locales").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 5l
      val versionTo = 6l

      val description = "Create local_nutrient_types"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """|CREATE TABLE local_nutrient_types
             |(
             |  id serial NOT NULL,
             |  locale_id character varying(16) NOT NULL,
             |  nutrient_type_id integer NOT NULL,
             |  CONSTRAINT local_nutrient_types_pk PRIMARY KEY (id),
             |  CONSTRAINT local_nutrient_types_locale_fk FOREIGN KEY(locale_id) REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE,
             |  CONSTRAINT local_nutrient_types_nutrient_type_fk FOREIGN KEY(nutrient_type_id) REFERENCES nutrient_types(id) ON UPDATE CASCADE ON DELETE CASCADE
             |)""".stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("DROP TABLE local_nutrient_types").execute()

        Right(())
      }
    },
    // 6 -> 7 see SystemV5_CreateLocalNutrientLists

    new Migration {
      val versionFrom = 7l
      val versionTo = 8l

      val description = "Add optional name, e-mail and phone fields to users"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE users ADD COLUMN name character varying(512)").execute()
        SQL("ALTER TABLE users ADD COLUMN email character varying(512)").execute()
        SQL("ALTER TABLE users ADD COLUMN phone character varying(32)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE users DROP COLUMN name").execute()
        SQL("ALTER TABLE users DROP COLUMN email").execute()
        SQL("ALTER TABLE users DROP COLUMN phone").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 8l
      val versionTo = 9l

      val description = "Add support_email to surveys"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE surveys ADD COLUMN support_email character varying(512) NOT NULL DEFAULT 'support@intake24.co.uk'").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE users DROP COLUMN support_email").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 9l
      val versionTo = 10l

      val description = "Create survey_support_staff"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """|CREATE TABLE survey_support_staff(
             |    survey_id character varying(64) NOT NULL,
             |    user_survey_id character varying(64) NOT NULL,
             |    user_id character varying(256) NOT NULL,
             |    sms_notifications boolean NOT NULL DEFAULT true,
             |    CONSTRAINT survey_support_staff_pk PRIMARY KEY (survey_id, user_survey_id, user_id),
             |    CONSTRAINT survey_support_staff_survey_id_fk FOREIGN KEY(survey_id) REFERENCES surveys(id) ON DELETE CASCADE ON UPDATE CASCADE,
             |    CONSTRAINT survey_support_staff_user_id_fk FOREIGN KEY(user_survey_id, user_id) REFERENCES users(survey_id, id) ON DELETE CASCADE ON UPDATE CASCADE
             |)""".stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("DROP TABLE survey_support_staff").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 10l
      val versionTo = 11l

      val description = "Drop support_staff and create global_support_staff instead"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("DROP TABLE support_staff").execute()

        SQL(
          """|CREATE TABLE global_support_staff(
             |    user_survey_id character varying(64) NOT NULL,
             |    user_id character varying(256) NOT NULL,
             |    sms_notifications boolean NOT NULL DEFAULT true,
             |    CONSTRAINT global_support_staff_pk PRIMARY KEY (user_survey_id, user_id),
             |    CONSTRAINT global_support_staff_user_id_fk FOREIGN KEY(user_survey_id, user_id) REFERENCES users(survey_id, id) ON DELETE CASCADE ON UPDATE CASCADE
             |)""".stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 11l
      val versionTo = 12l

      val description = "User survey_id as the primary key in gen_user_counter"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE gen_user_counters DROP CONSTRAINT gen_user_count_id_pk").execute()
        SQL("ALTER TABLE gen_user_counters ADD PRIMARY KEY(survey_id)").execute()
        SQL("ALTER TABLE gen_user_counters DROP COLUMN id").execute()

        Right(())

      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 12l
      val versionTo = 13l

      val description = "Copy user counter values from global_values table and drop it"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        val values = SQL("SELECT name, value FROM global_values")
          .executeQuery()
          .as((SqlParser.str("name") ~ SqlParser.str("value")).*)
          .filter(_._1.endsWith("_gen_user_counter"))
          .map(x => (x._1.replace("_gen_user_counter", ""), x._2.toInt))

        val params = values.map {
          case (surveyId, count) => Seq[NamedParameter]('survey_id -> surveyId, 'count -> count)
        }

        if (params.nonEmpty) {
          BatchSql("INSERT INTO gen_user_counters VALUES({survey_id}, {count}) ON CONFLICT(survey_id) DO UPDATE SET count={count}", params.head, params.tail: _*).execute()
        }

        SQL("DROP TABLE global_values").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 13l
      val versionTo = 14l

      val description = "Create gwt_client_error_reports table"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        val query =
          """
            |CREATE TABLE gwt_client_error_reports(
            |    id serial NOT NULL PRIMARY KEY,
            |    user_id character varying(256),
            |    survey_id character varying(64),
            |    reported_at timestamp NOT NULL,
            |    gwt_permutation_name character varying(256) NOT NULL,
            |    exception_chain_json text NOT NULL,
            |    survey_state_json text NOT NULL,
            |    new boolean NOT NULL DEFAULT true
            |)""".stripMargin

        SQL(query).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("DROP TABLE gwt_client_error_reports").execute()

        Right(())
      }
    },

    new Migration {
      val versionFrom = 14l
      val versionTo = 15l

      val description = "Rename gwt_client_error_reports to client_error_reports and drop GWT-specific fields"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE gwt_client_error_reports RENAME TO client_error_reports").execute()
        SQL("ALTER TABLE client_error_reports DROP COLUMN gwt_permutation_name").execute()
        SQL("ALTER TABLE client_error_reports RENAME COLUMN exception_chain_json TO stack_trace").execute()
        SQL("ALTER TABLE client_error_reports ALTER COLUMN stack_trace TYPE character varying(256)[] USING ARRAY[]::character varying(256)[]").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 15l
      val versionTo = 16l

      val description = "Add time zone to survey submission times"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE survey_submissions ALTER COLUMN start_time TYPE timestamp with time zone").execute()
        SQL("ALTER TABLE survey_submissions ALTER COLUMN end_time TYPE timestamp with time zone").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 16l
      val versionTo = 17l

      val description = "Convert survey submission log to array and delete old logs (they take up too much space and no one has ever used them)"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE survey_submissions ALTER COLUMN log TYPE text[] USING ARRAY[]::text[]").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 17l
      val versionTo = 18l

      val description = "Drop survey_submission_user_custom_fields"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("DROP TABLE survey_submission_user_custom_fields").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 18l
      val versionTo = 19l

      val description = "Drop unused help_requests and help_request_times tabels"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("DROP TABLE help_requests").execute()
        SQL("DROP TABLE last_help_request_times").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 19l
      val versionTo = 20l

      val description = "Add integer PK to users"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        def updateUserIdColumn(table: String, userSurveyIdColumnName: String) = {
          SQL(s"ALTER TABLE $table ADD COLUMN old_user_id character varying(256)").execute()
          SQL(s"UPDATE $table SET old_user_id=user_id").execute()
          SQL(s"ALTER TABLE $table ALTER COLUMN user_id DROP NOT NULL").execute()
          SQL(s"ALTER TABLE $table ALTER COLUMN user_id TYPE integer USING NULL").execute()
          SQL(s"UPDATE $table SET user_id=(SELECT id FROM users WHERE $table.$userSurveyIdColumnName=users.survey_id AND $table.old_user_id=users.old_id)").execute()
          SQL(s"ALTER TABLE $table ALTER COLUMN user_id SET NOT NULL").execute()
          SQL(s"ALTER TABLE $table DROP COLUMN old_user_id").execute()
        }

        def createForeignKeyCascadeDelete(table: String) = {
          SQL(s"ALTER TABLE $table ADD CONSTRAINT ${table}_user_id_fkey FOREIGN KEY(user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE").execute()
        }

        def createForeignKeyRestrictDelete(table: String) = {
          SQL(s"ALTER TABLE $table ADD CONSTRAINT ${table}_user_id_fkey FOREIGN KEY(user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT").execute()
        }

        def dropColumn(table: String, columnName: String) = {
          SQL(s"ALTER TABLE $table DROP COLUMN $columnName").execute()
        }


        SQL("ALTER TABLE users ADD COLUMN old_id character varying(256)").execute()
        SQL("UPDATE users set old_id = id").execute()

        SQL("ALTER TABLE user_roles DROP CONSTRAINT user_roles_users_fk").execute()
        SQL("ALTER TABLE user_permissions DROP CONSTRAINT user_permissions_users_fk").execute()
        SQL("ALTER TABLE user_custom_fields DROP CONSTRAINT user_custom_fields_users_fk").execute()
        SQL("ALTER TABLE survey_submissions DROP CONSTRAINT survey_submissions_users_fk").execute()
        SQL("ALTER TABLE external_test_users DROP CONSTRAINT external_test_users_survey_id_fk").execute()
        SQL("ALTER TABLE survey_support_staff DROP CONSTRAINT survey_support_staff_user_id_fk").execute()
        SQL("ALTER TABLE global_support_staff DROP CONSTRAINT global_support_staff_user_id_fk").execute()
        SQL("ALTER TABLE missing_foods DROP CONSTRAINT missing_foods_user_fk").execute()

        SQL("ALTER TABLE users DROP CONSTRAINT users_id_pk").execute()

        SQL("CREATE SEQUENCE user_id_seq").execute()
        SQL("ALTER TABLE users ALTER COLUMN id TYPE integer USING nextval('user_id_seq')").execute()
        SQL("ALTER TABLE users ALTER COLUMN id SET DEFAULT nextval('user_id_seq')").execute()
        SQL("ALTER SEQUENCE user_id_seq OWNED BY users.id").execute()
        SQL("ALTER TABLE users ADD PRIMARY KEY(id)").execute()

        SQL("ALTER TABLE global_support_staff DROP CONSTRAINT global_support_staff_pk").execute()
        SQL("ALTER TABLE survey_support_staff DROP CONSTRAINT survey_support_staff_pk").execute()

        Seq("user_roles", "user_permissions", "user_custom_fields", "survey_submissions",
          "external_test_users", "missing_foods").foreach(updateUserIdColumn(_, "survey_id"))

        Seq("survey_support_staff", "global_support_staff").foreach(updateUserIdColumn(_, "user_survey_id"))

        // It's OK to automatically delete dependent records from these tables
        Seq("user_roles", "user_permissions", "user_custom_fields", "survey_support_staff",
          "global_support_staff", "external_test_users").foreach(createForeignKeyCascadeDelete(_))

        // But these need to be deleted manually first to prevent unintentional data loss
        Seq("survey_submissions", "missing_foods").foreach(createForeignKeyRestrictDelete(_))

        Seq("user_custom_fields", "external_test_users").foreach(dropColumn(_, "survey_id")) // keep survey_id for "user_roles" and "user_permissions" to rewrite roles later

        Seq("survey_support_staff", "global_support_staff").foreach(dropColumn(_, "user_survey_id"))

        SQL("ALTER TABLE survey_support_staff ADD PRIMARY KEY(survey_id, user_id)").execute()
        SQL("ALTER TABLE global_support_staff ADD PRIMARY KEY(user_id)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 20l
      val versionTo = 21l

      val description = "Move survey aliases to a separate table"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """CREATE TABLE user_survey_aliases(
            |  user_id integer NOT NULL,
            |  survey_id character varying(32),
            |  user_name character varying(256),
            |  CONSTRAINT user_aliases_pkey PRIMARY KEY(survey_id, user_name),
            |  CONSTRAINT user_aliases_user_id_fkey FOREIGN KEY(user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
            |  CONSTRAINT user_aliases_survey_id_fkey FOREIGN KEY(survey_id) REFERENCES surveys(id) ON UPDATE CASCADE ON DELETE RESTRICT
            |)
          """.stripMargin).execute()

        SQL("INSERT INTO user_survey_aliases SELECT id, survey_id, old_id FROM users WHERE survey_id <> ''").execute()

        SQL("ALTER TABLE users DROP old_id, DROP survey_id").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 21l
      val versionTo = 22l

      val description = "Drop useless ids from roles and permissions"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE user_roles DROP COLUMN id").execute()
        SQL("ALTER TABLE user_roles ADD PRIMARY KEY(user_id, role)").execute()

        SQL("ALTER TABLE user_permissions DROP COLUMN id").execute()
        SQL("ALTER TABLE user_permissions ADD PRIMARY KEY(user_id, permission)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 22l
      val versionTo = 23l

      val description = "Create index on users.email"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("CREATE INDEX users_email_index ON users(email)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 23l
      val versionTo = 24l

      val description = "Create simple_name for searching on user names without accents and diacritics"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE users ADD COLUMN simple_name character varying(512)").execute()
        SQL("CREATE INDEX users_simple_name_index ON users(simple_name)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 24l
      val versionTo = 25l

      val description = "Add unique e-mail constraint"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE users ADD CONSTRAINT users_email_unique UNIQUE(email)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {
      val versionFrom = 25l
      val versionTo = 26l

      val description = "Convert old roles"

      def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE user_roles DROP CONSTRAINT no_duplicate_roles").execute()
        SQL("ALTER TABLE user_roles DROP CONSTRAINT user_roles_pkey").execute()
        SQL("UPDATE user_roles SET role=survey_id || '/' || role WHERE role='staff' OR role='respondent'").execute()
        SQL("DELETE FROM user_roles WHERE EXISTS (SELECT 1 FROM user_roles AS t WHERE t.user_id = user_roles.user_id AND t.role = user_roles.role AND t.ctid > user_roles.ctid)").execute()
        SQL("ALTER TABLE user_roles ADD CONSTRAINT user_roles_pkey PRIMARY KEY(user_id, role)").execute()
        SQL("ALTER TABLE user_roles DROP COLUMN survey_id").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 26l
      override val versionTo: Long = 27l
      override val description: String = "Add participants info"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """
            |CREATE TYPE sex_enum AS ENUM ('f', 'm');
            |
            |CREATE TABLE user_info (
            |   user_id integer PRIMARY KEY,
            |   first_name character varying(500),
            |   year_of_birth integer,
            |   sex sex_enum,
            |   weight_kg numeric(10,3),
            |   height_cm numeric(10,3),
            |   level_of_physical_activity_id integer,
            |   CONSTRAINT users_id_fk FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
            |   CONSTRAINT realistic_year_of_birth CHECK (year_of_birth > 1800 AND year_of_birth < 3000),
            |   CONSTRAINT realistic_weight CHECK (weight_kg > 0 AND weight_kg < 1000),
            |   CONSTRAINT realistic_height CHECK (height_cm > 0 AND height_cm < 500)
            |);
          """.stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL(
          """
            |DROP TABLE user_info;
            |DROP TYPE sex_enum;
          """.stripMargin).execute()
        Right()
      }
    },

    new Migration {

      override val versionFrom: Long = 27l
      override val versionTo: Long = 28l
      override val description: String = "Add survey ID special characters constraint"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("DELETE FROM surveys WHERE id=''").execute()

        SQL("ALTER TABLE surveys ADD CONSTRAINT surveys_id_characters CHECK (id ~ '\\^[A-Za-z0-9_-]+$')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 28l
      override val versionTo: Long = 29l
      override val description: String = "Move user passwords to a separate table"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """CREATE TABLE user_passwords(
            |  user_id integer NOT NULL PRIMARY KEY,
            |  password_hash character varying(128) NOT NULL,
            |  password_salt character varying(128) NOT NULL,
            |  password_hasher character varying(64) NOT NULL,
            |  CONSTRAINT user_passwords_user_id_fkey FOREIGN KEY(user_id) REFERENCES users(id)
            |    ON UPDATE CASCADE ON DELETE CASCADE
            |)
          """.stripMargin).execute()

        SQL("INSERT INTO user_passwords(user_id, password_hash, password_salt, password_hasher) SELECT id, password_hash, password_salt, password_hasher FROM users").execute()

        SQL("ALTER TABLE users DROP COLUMN password_hash, DROP COLUMN password_salt, DROP COLUMN password_hasher").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 29l
      override val versionTo: Long = 30l
      override val description: String = "Add url_auth_token to user_survey_aliases"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE user_survey_aliases ADD COLUMN url_auth_token character varying(32)").execute()
        SQL("ALTER TABLE user_survey_aliases ADD CONSTRAINT user_survey_aliases_auth_token_unique UNIQUE(url_auth_token)").execute()
        SQL("CREATE INDEX user_survey_aliases_auth_token ON user_survey_aliases(url_auth_token)").execute()

        val aliases = SQL("SELECT survey_id, user_name FROM user_survey_aliases").executeQuery().as((SqlParser.str("survey_id") ~ SqlParser.str("user_name")).*)

        val params = aliases.map {
          case surveyId ~ userName =>
            Seq[NamedParameter]('survey_id -> surveyId, 'user_name -> userName, 'url_auth_token -> URLAuthTokenUtils.generateToken)
        }

        if (params.nonEmpty)
          BatchSql("UPDATE user_survey_aliases SET url_auth_token = {url_auth_token} WHERE survey_id = {survey_id} AND user_name={user_name}", params.head, params.tail: _*).execute()

        SQL("ALTER TABLE user_survey_aliases ALTER COLUMN url_auth_token SET NOT NULL")

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 30l
      override val versionTo: Long = 31l
      override val description: String = "Create signin_log table"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """
            |CREATE TABLE signin_log(
            |  id serial NOT NULL PRIMARY KEY,
            |  date timestamp with time zone NOT NULL DEFAULT now(),
            |  remote_address character varying(64) NOT NULL,
            |  provider character varying(64) NOT NULL,
            |  provider_key character varying(512) NOT NULL,
            |  successful boolean NOT NULL,
            |  user_id integer,
            |  message text
            |)
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
      override val description: String = "Rename user_info to user_physical_data"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE user_info RENAME TO user_physical_data".stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 32l
      override val versionTo: Long = 33l
      override val description: String = "Delete global_support_staff tables and use roles instead"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("INSERT INTO user_roles(user_id, role) SELECT user_id,'globalsupport' FROM global_support_staff").execute()
        SQL("DROP TABLE global_support_staff").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 33l
      override val versionTo: Long = 34l
      override val description: String = "Delete survey_support_staff tables and use roles instead"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("INSERT INTO user_roles(user_id, role) SELECT user_id, survey_id || '/support' FROM survey_support_staff").execute()
        SQL("DROP TABLE survey_support_staff").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 34l
      override val versionTo: Long = 35l
      override val description: String = "Add sms_notifications and email_notifications to user profile"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE users ADD COLUMN email_notifications boolean NOT NULL DEFAULT true").execute()
        SQL("ALTER TABLE users ADD COLUMN sms_notifications boolean NOT NULL DEFAULT true").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 35l
      override val versionTo: Long = 36l
      override val description: String = "Add user_agent to signin_log"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE signin_log ADD COLUMN user_agent character varying(512)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 36l
      override val versionTo: Long = 37l
      override val description: String = "Add birthdate to user_physical_data"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """
            |ALTER TABLE user_physical_data ADD COLUMN birthdate date;
            |ALTER TABLE user_physical_data DROP CONSTRAINT realistic_year_of_birth;
            |ALTER TABLE user_physical_data DROP COLUMN year_of_birth;
          """.stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL(
          """
            |ALTER TABLE user_physical_data DROP COLUMN birthdate;
            |ALTER TABLE user_physical_data ADD COLUMN year_of_birth integer;
            |ALTER TABLE user_physical_data ADD CONSTRAINT realistic_year_of_birth CHECK (year_of_birth > 1800 AND year_of_birth < 3000);
          """.stripMargin).execute()

        Right(())
      }
    },

    new Migration {

      override val versionFrom: Long = 37l
      override val versionTo: Long = 38l
      override val description: String = "Create survey_submission_missing_foods"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """
            |CREATE table survey_submission_missing_foods (
            |  id serial NOT NULL,
            |  meal_id integer NOT NULL,
            |  name character varying(512) NOT NULL,
            |  brand character varying(512) NOT NULL,
            |  description character varying(512) NOT NULL,
            |  portion_size character varying(512) NOT NULL,
            |  leftovers character varying(512) NOT NULL,
            |  CONSTRAINT survey_submission_missing_foods_fkey FOREIGN KEY(meal_id) REFERENCES survey_submission_meals(id)
            |)
          """.stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 38l
      override val versionTo: Long = 39l
      override val description: String = "Add originating_url to surveys"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE surveys ADD COLUMN originating_url character varying(512)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 39l
      override val versionTo: Long = 40l
      override val description: String = "Survey descriptions"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE surveys ADD COLUMN description character varying(10000)").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 40l
      override val versionTo: Long = 41l
      override val description: String = "Add feedback_enabled flag to surveys"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("ALTER TABLE surveys ADD COLUMN feedback_enabled boolean NOT NULL DEFAULT false").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 41l
      override val versionTo: Long = 42l
      override val description: String = "Create case-insensitive e-mail index"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL("ALTER TABLE users DROP CONSTRAINT users_email_unique").execute()
        SQL("DROP INDEX users_email_index").execute()

        SQL("CREATE UNIQUE INDEX users_email_case_insensitive_index ON users (lower(email))").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 42l
      override val versionTo: Long = 43l
      override val description: String = "Drop first_name user_physical_data"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL(
          """
            |ALTER TABLE user_physical_data DROP COLUMN first_name;
          """.stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 43l
      override val versionTo: Long = 44l
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

      override val versionFrom: Long = 44l
      override val versionTo: Long = 45l
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

      override val versionFrom: Long = 45l
      override val versionTo: Long = 46l
      override val description: String = "Add weight target"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

        SQL(
          """
            |CREATE TYPE weight_target_enum AS ENUM ('keep_weight', 'loose_weight', 'gain_weight');
            |
            |ALTER TABLE user_physical_data
            |ADD COLUMN weight_target weight_target_enum;
            |
            |ALTER TABLE user_physical_data
            |RENAME COLUMN level_of_physical_activity_id TO physical_activity_level_id;
          """.stripMargin).execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL(
          """
            |ALTER TABLE user_physical_data
            |DROP COLUMN weight_target;
            |
            |ALTER TABLE user_physical_data
            |RENAME COLUMN physical_activity_level_id TO level_of_physical_activity_id;
            |
            |DROP TYPE weight_target_enum;
          """.stripMargin).execute()
        Right(())
      }
    },

    AlterWeightTargetEnum,

    new Migration {

      override val versionFrom: Long = 47l
      override val versionTo: Long = 48l
      override val description: String = "Add New Zealand locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('en_NZ', 'English (New Zealand)', 'English (New Zealand)', 'en_GB', 'en', 'nz', 'en_GB')").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???

      }
    },

    new Migration {

      override val versionFrom: Long = 48l
      override val versionTo: Long = 49l
      override val description: String = "Create en_NZ local nutrients list from en_GB"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO local_nutrient_types(locale_id, nutrient_type_id) SELECT 'en_NZ', nutrient_type_id FROM local_nutrient_types WHERE locale_id='en_GB'").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 49l
      override val versionTo: Long = 50l
      override val description: String = "Create ar_AE local nutrient list from en_GB"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO local_nutrient_types(locale_id, nutrient_type_id) SELECT 'ar_AE', nutrient_type_id FROM local_nutrient_types WHERE locale_id='en_GB'").execute()

        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    },

    new Migration {

      override val versionFrom: Long = 50l
      override val versionTo: Long = 51l
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

      override val versionFrom: Long = 51l
      override val versionTo: Long = 52l
      override val description: String = "Add a UK gluten-free foods locale"

      override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        SQL("INSERT INTO locales VALUES('en_GB_gf', 'United Kingdom (gluten-free foods)', 'United Kingdom (gluten-free foods)', 'en_GB', 'en', 'gb', 'en_GB')").execute()


        Right(())
      }

      def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
        ???
      }
    }


  )

}
