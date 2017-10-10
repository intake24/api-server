package uk.ac.ncl.openlab.intake24.systemsql.migrations

import java.sql.Connection
import java.util.UUID

import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}
import anorm.{BatchSql, Macro, NamedParameter, SQL, SqlParser}
import uk.ac.ncl.openlab.intake24.pairwiseAssociationRules.PairwiseAssociationRules

/**
  * Created by Tim Osadchiy on 29/09/2017.
  */
object PairwiseAssociationsMigration extends Migration {

  override val versionFrom: Long = 62l
  override val versionTo: Long = 63l

  override val description: String = "Creating and filling table for Pairwise Associations"

  override def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
    createTables()
    fillTables()
    Right(())
  }

  override def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {
    SQL(
      """
        |DROP TABLE pairwise_associations_occurrence;
        |DROP TABLE pairwise_associations_co_occurrences;
        |DROP TABLE pairwise_associations_transactions_count;
      """.stripMargin).execute()
    Right(())
  }

  private def createTables()(implicit connection: Connection) = {
    //    FIXME: Drop table pairwise_associations_transactions_count
    SQL(
      """
        |CREATE TABLE pairwise_associations_occurrences (
        |  locale      CHARACTER VARYING(64) NOT NULL,
        |  food_code   CHARACTER VARYING(50) NOT NULL,
        |  occurrences INTEGER               NOT NULL,
        |  PRIMARY KEY (locale, food_code)
        |);
        |
        |CREATE TABLE pairwise_associations_co_occurrences (
        |  locale          CHARACTER VARYING(64) NOT NULL,
        |  antecedent_food_code CHARACTER VARYING(50) NOT NULL,
        |  consequent_food_code CHARACTER VARYING(50) NOT NULL,
        |  occurrences     INTEGER               NOT NULL,
        |  PRIMARY KEY (locale, antecedent_food_code, consequent_food_code)
        |);
        |
        |CREATE TABLE pairwise_associations_transactions_count (
        |  locale             CHARACTER VARYING(64) PRIMARY KEY,
        |  transactions_count INTEGER NOT NULL
        |);
      """.stripMargin).execute()
  }

  private def fillTables()(implicit connection: Connection) = {
    val graph = getOccurrenceGraph()
    println("Updating db...")
    val occurrenceUpdateParams = graph.flatMap { localeNode =>
      localeNode._2.getParams().occurrences.map { ocNode =>
        Seq[NamedParameter]('locale -> localeNode._1, 'food_code -> ocNode._1, 'occurrences -> ocNode._2)
      }
    }.toSeq
    val coOccurrenceUpdateParams = graph.flatMap { localeNode =>
      localeNode._2.getParams().coOccurrences.flatMap { ocNode =>
        ocNode._2.map { consItemNode =>
          Seq[NamedParameter]('locale -> localeNode._1, 'antecedent_food_code -> ocNode._1, 'consequent_food_code -> consItemNode._1, 'occurrences -> consItemNode._2)
        }
      }
    }.toSeq

    if (occurrenceUpdateParams.nonEmpty && coOccurrenceUpdateParams.nonEmpty) {
      BatchSql(
        """
          |INSERT INTO pairwise_associations_occurrences (locale, food_code, occurrences)
          |VALUES ({locale}, {food_code}, {occurrences});
        """.stripMargin, occurrenceUpdateParams.head, occurrenceUpdateParams.tail: _*).execute()

      BatchSql(
        """
          |INSERT INTO pairwise_associations_co_occurrences (locale, antecedent_food_code, consequent_food_code, occurrences)
          |VALUES ({locale}, {antecedent_food_code}, {consequent_food_code}, {occurrences});
        """.stripMargin, coOccurrenceUpdateParams.head, coOccurrenceUpdateParams.tail: _*).execute()
    }
    println("Done")
  }

  private def getOccurrenceGraph()(implicit connection: Connection) = {
    val minSubmissionCount = 50
    val batchSize = 50
    val occurrenceGraph = Map[String, PairwiseAssociationRules]().withDefaultValue(PairwiseAssociationRules(None))

    getSurveyIds().foldLeft(occurrenceGraph) { (ocMp, surveyId) =>
      if (surveyId.toLowerCase().contains("test")) {
        println(s"Survey $surveyId is ignored since it contains word 'test'")
        ocMp
      } else {
        val submissionCount = countSurveySubmissions(surveyId)
        if (submissionCount < minSubmissionCount) {
          println(s"Survey $surveyId is ignored since it has less than $minSubmissionCount submissions")
          ocMp
        } else {
          println(s"Processing survey $surveyId")
          Range(0, submissionCount, batchSize).foldLeft(ocMp) { (ocMp, offset) =>
            val subs = getSubmissions(surveyId, offset, batchSize).foldLeft(ocMp)(addSubmissionToOccurrenceGraph)
            println(s"  processed ${offset + subs.size} out of $submissionCount")
            subs
          }
        }
      }
    }
  }

  private def addSubmissionToOccurrenceGraph(graph: Map[String, PairwiseAssociationRules], submission: Submission): Map[String, PairwiseAssociationRules] =
    submission.meals.foldLeft(graph) { (ocMp, meal) =>
      ocMp + (submission.locale -> {
        val graph = ocMp(submission.locale)
        graph.addTransaction(meal)
        graph
      })
    }

  private def getSurveyIds()(implicit connection: Connection) = {
    SQL(
      """
        |SELECT id FROM surveys;
      """.stripMargin).executeQuery().as(SqlParser.str("id").*)
  }

  private def countSurveySubmissions(surveyId: String)(implicit connection: Connection) = {
    val countQuery =
      """|SELECT count(*) FROM survey_submissions AS ss
         |WHERE ss.survey_id={survey_id};
         |""".stripMargin

    SQL(countQuery).on('survey_id -> surveyId).executeQuery().as(SqlParser.int(1).single)
  }

  private def getSubmissions(surveyId: String, offset: Int, limit: Int)(implicit connection: Connection): List[Submission] = {
    getSubmissionIds(surveyId, offset, limit).foldLeft(List[Submission]()) {
      (acc, submissionId) =>
        getSubmittedFoods(submissionId) match {
          case Nil => acc
          case l =>
            val meals = l.groupBy(_.meal_id).map(m => m._2.map(_.food_code)).toSeq
            Submission(l.head.locale, meals) :: acc
        }
    }
  }

  private def getSubmissionIds(surveyId: String, offset: Int, limit: Int)(implicit connection: Connection) = {
    val q =
      """
        |SELECT survey_submissions.id
        |FROM survey_submissions
        |  JOIN surveys ON surveys.id = survey_submissions.survey_id
        |WHERE surveys.id = {survey_id}
        |ORDER BY survey_submissions.end_time ASC
        |OFFSET {offset}
        |LIMIT {limit}
      """.stripMargin
    SQL(q).on('survey_id -> surveyId, 'offset -> offset, 'limit -> limit).executeQuery().as(SqlParser.scalar(anorm.Column.columnToUUID).*)
  }

  private def getSubmittedFoods(submissionId: UUID)(implicit connection: Connection) = {
    val q =
      """|SELECT
         |  surveys.locale                      AS locale,
         |  survey_submission_meals.id          AS meal_id,
         |  survey_submission_foods.code        AS food_code
         |FROM surveys
         |  JOIN survey_submissions ON surveys.id = survey_submissions.survey_id
         |  JOIN survey_submission_meals ON survey_submission_meals.survey_submission_id = survey_submissions.id
         |  JOIN survey_submission_foods ON survey_submission_foods.meal_id = survey_submission_meals.id
         |WHERE survey_submissions.id = {submission_id}::uuid""".stripMargin
    SQL(q).on('submission_id -> submissionId).executeQuery().as(Macro.namedParser[SubmittedFoodRow].*)


  }

  private case class SubmittedFoodRow(locale: String, meal_id: Int, food_code: String)

  private case class Submission(locale: String, meals: Seq[Seq[String]])

}
