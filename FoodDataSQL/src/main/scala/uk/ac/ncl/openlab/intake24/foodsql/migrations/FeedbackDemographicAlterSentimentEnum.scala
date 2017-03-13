package uk.ac.ncl.openlab.intake24.foodsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by Tim Osadchiy on 01/03/2017.
  */
object FeedbackDemographicAlterSentimentEnum extends Migration {

  val versionFrom = 26l
  val versionTo = 27l

  val description = "Change sentiments from emotions to amount sentiments"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |ALTER TYPE sentiment_enum RENAME TO old_sentiment_enum;
        |
        |ALTER TABLE demographic_group_scale_sector ALTER COLUMN sentiment DROP DEFAULT;
        |
        |CREATE TYPE sentiment_enum
        |AS enum('highly_negative', 'negative', 'warning', 'neutral', 'positive', 'highly_positive',
        |        'too_low', 'low', 'bit_low', 'good', 'excellent', 'bit_high', 'high', 'too_high');
        |
        |ALTER TABLE demographic_group_scale_sector ALTER COLUMN sentiment TYPE sentiment_enum
        |USING sentiment::text::sentiment_enum;
        |
        |DROP TYPE old_sentiment_enum;
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'too_low'
        |WHERE dgs.sentiment = 'highly_negative';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'low'
        |WHERE dgs.sentiment = 'negative';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'bit_low'
        |WHERE dgs.sentiment = 'warning';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'good'
        |WHERE dgs.sentiment = 'neutral';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'good'
        |WHERE dgs.sentiment = 'positive';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'excellent'
        |WHERE dgs.sentiment = 'highly_positive';
        |
        |ALTER TYPE sentiment_enum RENAME TO old_sentiment_enum;
        |
        |CREATE TYPE sentiment_enum
        |AS enum('too_low', 'low', 'bit_low', 'good', 'excellent', 'bit_high', 'high', 'too_high');
        |
        |ALTER TABLE demographic_group_scale_sector ALTER COLUMN sentiment TYPE sentiment_enum
        |USING sentiment::text::sentiment_enum;
        |
        |DROP TYPE old_sentiment_enum;
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |ALTER TYPE sentiment_enum RENAME TO old_sentiment_enum;
        |
        |CREATE TYPE sentiment_enum
        |AS enum('highly_negative', 'negative', 'warning', 'neutral', 'positive', 'highly_positive',
        |        'too_low', 'low', 'bit_low', 'good', 'excellent', 'bit_high', 'high', 'too_high');
        |
        |ALTER TABLE demographic_group_scale_sector ALTER COLUMN sentiment TYPE sentiment_enum
        |USING sentiment::text::sentiment_enum;
        |
        |DROP TYPE old_sentiment_enum;
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'highly_negative'
        |WHERE dgs.sentiment = 'too_low';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'negative'
        |WHERE dgs.sentiment = 'low';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'warning'
        |WHERE dgs.sentiment = 'bit_low';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'neutral'
        |WHERE dgs.sentiment = 'good';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'positive'
        |WHERE dgs.sentiment = 'good';
        |
        |UPDATE demographic_group_scale_sector AS dgs SET sentiment = 'highly_positive'
        |WHERE dgs.sentiment = 'excellent';
        |
        |ALTER TYPE sentiment_enum RENAME TO old_sentiment_enum;
        |
        |CREATE TYPE sentiment_enum
        |AS enum('highly_negative', 'negative', 'warning', 'neutral', 'positive', 'highly_positive');
        |
        |ALTER TABLE demographic_group_scale_sector ALTER COLUMN sentiment TYPE sentiment_enum
        |USING sentiment::text::sentiment_enum;
        |
        |DROP TYPE old_sentiment_enum;
        |
        |ALTER TABLE demographic_group_scale_sector ALTER COLUMN sentiment SET DEFAULT 'neutral';
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

}