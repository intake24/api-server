package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.time.{Instant, ZonedDateTime}
import java.util.UUID
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}
import uk.ac.ncl.openlab.intake24.surveydata._

class DataExportImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends DataExportService with SqlDataService with SqlResourceLoader {

  lazy val getSurveySubmissionsSql = sqlFromResource("admin/get_survey_submissions.sql")

  lazy val getSurveySubmissionMealsSql = sqlFromResource("admin/get_survey_submission_meals.sql")

  lazy val getSurveySubmissionFoodsSql = sqlFromResource("admin/get_survey_submission_foods.sql")

  lazy val getSurveySubmissionMissingFoodsSql = sqlFromResource("admin/get_survey_submission_missing_foods.sql")

  lazy val getSurveySubmissionNutrientsSql = sqlFromResource("admin/get_survey_submission_nutrients.sql")

  private case class SubmissionRow(id: UUID, survey_id: String, user_id: Int, user_name: Option[String], start_time: ZonedDateTime, end_time: ZonedDateTime, log: Array[String],
                                   submission_custom_fields: Array[Array[String]], user_custom_fields: Array[Array[String]])

  private case class MealRow(submission_id: UUID, meal_id: Long, hours: Int, minutes: Int, name: String, custom_fields: Array[Array[String]])

  private case class FoodRow(meal_id: Long, food_id: Long, code: String, english_description: String, local_description: Option[String], ready_meal: Boolean, search_term: String,
                             portion_size_method_id: String, reasonable_amount: Boolean, food_group_id: Int, brand: String, nutrient_table_id: String, nutrient_table_code: String,
                             custom_fields: Array[Array[String]], portion_size_data: Array[Array[String]])

  private case class MissingFoodRow(meal_id: Long, name: String, brand: String, description: String, portion_size: String, leftovers: String)

  private case class NutrientRow(food_id: Long, n_type: Long, n_amount: Double)

  private def customFieldsAsMap(fields: Array[Array[String]]) =
    fields.foldLeft(Map[String, String]()) {
      case (acc, cf) => acc + (cf(0) -> cf(1))
    }

  def getSurveySubmissions(surveyId: String, dateFrom: Option[ZonedDateTime], dateTo: Option[ZonedDateTime], offset: Int, limit: Int, respondentId: Option[Long]): Either[LookupError, Seq[ExportSubmission]] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).isDefined

        if (surveyExists) {

          val submissionRows = SQL(getSurveySubmissionsSql)
            .on('survey_id -> surveyId,
              'time_from -> dateFrom,
              'time_to -> dateTo,
              'offset -> offset,
              'limit -> limit,
              'respondent_id -> respondentId)
            .executeQuery()
            .as(Macro.namedParser[SubmissionRow].*)

          val submissionIds = submissionRows.map(_.id)

          val submissionIdsSeqParam = SeqParameter(submissionIds, post = "::uuid")

          if (submissionIds.isEmpty)
            Right(Seq())
          else {
            val mealRows = SQL(getSurveySubmissionMealsSql).on('submission_ids -> submissionIdsSeqParam).executeQuery().as(Macro.namedParser[MealRow].*).groupBy(_.submission_id)

            val foodRows = SQL(getSurveySubmissionFoodsSql).on('submission_ids -> submissionIdsSeqParam).executeQuery().as(Macro.namedParser[FoodRow].*).groupBy(_.meal_id)

            val missingFoodRows = SQL(getSurveySubmissionMissingFoodsSql).on('submission_ids -> submissionIdsSeqParam).executeQuery().as(Macro.namedParser[MissingFoodRow].*).groupBy(_.meal_id)

            val nutrientRows = SQL(getSurveySubmissionNutrientsSql).on('submission_ids -> submissionIdsSeqParam).executeQuery().as(Macro.namedParser[NutrientRow].*).groupBy(_.food_id)

            val submissions = submissionRows.map {
              submissionRow =>
                val meals = mealRows.getOrElse(submissionRow.id, Seq()).map {
                  mealRow =>
                    val foods = foodRows.getOrElse(mealRow.meal_id, Seq()).map {
                      foodRow =>
                        val nutrients = nutrientRows.getOrElse(foodRow.food_id, Seq()).map {
                          nutrientRow =>
                            (nutrientRow.n_type.toInt, nutrientRow.n_amount)
                        }.toMap

                        ExportFood(foodRow.code, foodRow.english_description, foodRow.local_description, foodRow.search_term, foodRow.nutrient_table_id, foodRow.nutrient_table_code, foodRow.ready_meal,
                          PortionSize(foodRow.portion_size_method_id, customFieldsAsMap(foodRow.portion_size_data)), foodRow.reasonable_amount,
                          foodRow.food_group_id, foodRow.brand, nutrients, customFieldsAsMap(foodRow.custom_fields))
                    }

                    val missingFoods = missingFoodRows.getOrElse(mealRow.meal_id, Seq()).map {
                      mfr =>
                        MissingFood(mfr.name, mfr.brand, mfr.description, mfr.portion_size, mfr.leftovers)
                    }

                    ExportMeal(mealRow.name, MealTime(mealRow.hours, mealRow.minutes), customFieldsAsMap(mealRow.custom_fields), foods, missingFoods)
                }
                ExportSubmission(submissionRow.id, submissionRow.user_id, submissionRow.user_name, customFieldsAsMap(submissionRow.user_custom_fields), customFieldsAsMap(submissionRow.submission_custom_fields),
                  submissionRow.start_time, submissionRow.end_time, meals)
            }

            Right(submissions)
          }
        } else
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  def createExportTask(parameters: ExportTaskParameters): Either[UnexpectedDatabaseError, Long] = tryWithConnection {
    implicit conn =>
      Right(SQL("INSERT INTO data_export_tasks(id, survey_id, time_from, time_to, user_id, created_at VALUES(DEFAULT, {survey_id}, {time_from}, {time_to}, {user_id}, NOW())")
        .on('survey_id -> parameters.surveyId, 'date_from -> parameters.dateFrom, 'date_to -> parameters.dateTo, 'user_id -> parameters.userId)
        .executeInsert())
  }

  def updateExportTaskProgress(taskId: Long, progress: Double): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      if (SQL("UPDATE data_export_tasks SET progress={progress} WHERE id={task_id}")
        .on('task_id -> taskId, 'progress -> progress)
        .executeUpdate() == 1)
        Right(())
      else
        Left(RecordNotFound(new RuntimeException(s"Export task id $taskId does not exist")))
  }

  def setExportTaskStarted(taskId: Long): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      if (SQL("UPDATE data_export_tasks SET started_at=NOW() WHERE id={id}").on('id -> taskId).executeUpdate() == 1)
        Right(())
      else
        Left(RecordNotFound(new RuntimeException(s"Export task id $taskId does not exist")))
  }

  def setExportTaskSuccess(taskId: Long, downloadUrl: String): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      if (SQL("UPDATE data_export_tasks SET successful=true,download_url={url} WHERE id={task_id}")
        .on('task_id -> taskId, 'url -> downloadUrl)
        .executeUpdate() == 1)
        Right(())
      else
        Left(RecordNotFound(new RuntimeException(s"Export task id $taskId does not exist")))
  }

  def setExportTaskFailure(taskId: Long, cause: Throwable): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>

      def collectStackTrace(throwable: Throwable, stackTrace: List[String] = List()): List[String] = {
        if (throwable == null)
          stackTrace.reverse
        else {
          val exceptionDesc = s"${throwable.getClass().getName()}: ${throwable.getMessage()}"

          val withDesc = if (!stackTrace.isEmpty)
            s"Caused by $exceptionDesc" :: stackTrace
          else
            s"Exception $exceptionDesc" :: stackTrace

          val trace = throwable.getStackTrace.foldLeft(withDesc) {
            (st, ste) => s"  at ${ste.getClassName()}.${ste.getMethodName()}(${ste.getFileName()}:${ste.getLineNumber()})" :: st
          }

          collectStackTrace(throwable.getCause, trace)
        }
      }

      val stackTrace = collectStackTrace(cause)

      if (SQL("UPDATE data_export_tasks SET successful=false,stack_trace={stack_trace},completed_at=NOW() WHERE id={id}")
        .on('id -> taskId, 'stack_trace -> stackTrace.toArray)
        .executeUpdate() == 1)
        Right(())
      else Left(RecordNotFound(new RuntimeException(s"Export task id $taskId does not exist")))
  }

  private case class ExportTaskStatusRow(id: Long, progress: Option[Double], successful: Option[Boolean], download_url: Option[String])

  def getExportTaskStatus(taskId: Long): Either[LookupError, ExportTaskStatus] = tryWithConnection {
    implicit conn =>
      SQL("SELECT id, progress, successful, download_url FROM data_export_tasks WHERE id={id}")
        .on('id -> taskId)
        .as(Macro.namedParser[ExportTaskStatusRow].singleOpt) match {
        case Some(row) => Right(ExportTaskStatus(row.id, row.progress, row.successful, row.download_url))
        case None => Left(RecordNotFound(new RuntimeException(s"Export task id $taskId does not exist")))
      }
  }
}
