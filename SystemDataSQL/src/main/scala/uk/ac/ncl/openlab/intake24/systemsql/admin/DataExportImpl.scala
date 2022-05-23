package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.net.URL
import java.time.{Instant, ZonedDateTime}
import java.util.UUID
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}
import uk.ac.ncl.openlab.intake24.surveydata._
import uk.ac.ncl.openlab.intake24.errors.ErrorUtils.collectStackTrace

class DataExportImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends DataExportService with SqlDataService with SqlResourceLoader {

  val logger = LoggerFactory.getLogger(classOf[DataExportImpl])


  lazy val getSurveySubmissionsSql = sqlFromResource("admin/get_survey_submissions.sql")

  lazy val getSurveySubmissionMealsSql = sqlFromResource("admin/get_survey_submission_meals.sql")

  lazy val getSurveySubmissionFoodsSql = sqlFromResource("admin/get_survey_submission_foods.sql")

  lazy val getSurveySubmissionMissingFoodsSql = sqlFromResource("admin/get_survey_submission_missing_foods.sql")

  lazy val getSurveySubmissionNutrientsSql = sqlFromResource("admin/get_survey_submission_nutrients.sql")

  lazy val getSurveySubmissionFieldsSql = sqlFromResource("admin/get_survey_submission_fields.sql")

  private case class SubmissionRow(id: UUID, survey_id: String, user_id: Int, user_name: Option[String], start_time: ZonedDateTime, end_time: ZonedDateTime,
                                   submission_time: ZonedDateTime, log: Array[String], submission_custom_fields: Array[Array[String]], user_custom_fields: Array[Array[String]])

  private case class MealRow(submission_id: UUID, meal_id: Long, hours: Int, minutes: Int, name: String, custom_fields: Array[Array[String]])

  private case class FoodRow(meal_id: Long, food_id: Long, code: String, english_description: String, local_description: Option[String], ready_meal: Boolean, search_term: String,
                             portion_size_method_id: String, reasonable_amount: Boolean, food_group_id: Int, brand: String, nutrient_table_id: String, nutrient_table_code: String,
                             custom_fields: Array[Array[String]], portion_size_data: Array[Array[String]])

  private case class MissingFoodRow(meal_id: Long, mf_food_id: Long, name: String, brand: String, description: String, portion_size: String, leftovers: String)

  private case class NutrientRow(food_id: Long, n_type: Long, n_amount: Double)

  private case class FieldRow(food_id: Long, field_name: String, value: String)

  private def customFieldsAsMap(fields: Array[Array[String]]) =
    fields.foldLeft(Map[String, String]()) {
      case (acc, cf) => acc + (cf(0) -> cf(1))
    }

  def getSurveySubmissions(surveyId: String, dateFrom: Option[ZonedDateTime], dateTo: Option[ZonedDateTime], offset: Int, limit: Int, respondentId: Option[Long]): Either[LookupError, Seq[ExportSubmission]] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).isDefined

        if (surveyExists) {

          val t0 = System.currentTimeMillis()

          val submissionRows = SQL(getSurveySubmissionsSql)
            .on('survey_id -> surveyId,
              'time_from -> dateFrom,
              'time_to -> dateTo,
              'offset -> offset,
              'limit -> limit,
              'respondent_id -> respondentId)
            .executeQuery()
            .as(Macro.namedParser[SubmissionRow].*)

          logger.debug(s"Get submission rows time: ${System.currentTimeMillis() - t0} ms")

          val submissionIds = submissionRows.map(_.id)

          val submissionIdsSeqParam = SeqParameter(submissionIds, post = "::uuid")

          if (submissionIds.isEmpty)
            Right(Seq())
          else {

            val t1 = System.currentTimeMillis()
            val mealRows = SQL(getSurveySubmissionMealsSql).on('submission_ids -> submissionIdsSeqParam).executeQuery().as(Macro.namedParser[MealRow].*).groupBy(_.submission_id)
            logger.debug(s"Get meal rows time: ${System.currentTimeMillis() - t1} ms")

            val t2 = System.currentTimeMillis()
            val foodRows = SQL(getSurveySubmissionFoodsSql).on('submission_ids -> submissionIdsSeqParam).executeQuery().as(Macro.namedParser[FoodRow].*)
            val foodRowsGrouped = foodRows.groupBy(_.meal_id)
            logger.debug(s"Get food rows time: ${System.currentTimeMillis() - t2} ms, food rows count: ${foodRows.size}")

            val t3 = System.currentTimeMillis()
            val missingFoodRows = SQL(getSurveySubmissionMissingFoodsSql).on('submission_ids -> submissionIdsSeqParam).executeQuery().as(Macro.namedParser[MissingFoodRow].*).groupBy(_.meal_id)
            logger.debug(s"Get missing food rows time: ${System.currentTimeMillis() - t3} ms")

            val foodIds = foodRows.map(_.food_id)

            val t4 = System.currentTimeMillis()
            val nutrientRows = SQL("SELECT food_id, nutrient_type_id as n_type, amount as n_amount FROM survey_submission_nutrients WHERE food_id IN({food_ids})")
              .on('food_ids -> foodIds).executeQuery().as(Macro.namedParser[NutrientRow].*)
            val nutrientRowsGrouped = nutrientRows.groupBy(_.food_id)
            logger.debug(s"Get nutrient rows query time: ${System.currentTimeMillis() - t4} ms, nutrient rows count: ${nutrientRows.size}")

            val t5 = System.currentTimeMillis()
            val fieldRows = SQL("SELECT food_id, field_name, value FROM survey_submission_fields WHERE food_id IN({food_ids})").on('food_ids -> foodIds)
              .executeQuery()
              .as(Macro.namedParser[FieldRow].*)
            val fieldRowsGrouped = fieldRows.groupBy(_.food_id)
            logger.debug(s"Get field rows time: ${System.currentTimeMillis() - t5} ms, field rows count: ${fieldRows.size}")

            val t6 = System.currentTimeMillis()

            val submissions = submissionRows.map {
              submissionRow =>
                val meals = mealRows.getOrElse(submissionRow.id, Seq()).map {
                  mealRow =>
                    val foods = foodRowsGrouped.getOrElse(mealRow.meal_id, Seq()).map {
                      foodRow =>
                        val nutrients = nutrientRowsGrouped.getOrElse(foodRow.food_id, Seq()).map {
                          nutrientRow =>
                            (nutrientRow.n_type.toInt, nutrientRow.n_amount)
                        }.toMap

                        val fields = fieldRowsGrouped.getOrElse(foodRow.food_id, Seq()).map {
                          fieldRow =>
                            (fieldRow.field_name, fieldRow.value)
                        }.toMap


                        ExportFood(foodRow.food_id, foodRow.code, foodRow.english_description, foodRow.local_description, foodRow.search_term, foodRow.nutrient_table_id, foodRow.nutrient_table_code, foodRow.ready_meal,
                          PortionSize(foodRow.portion_size_method_id, customFieldsAsMap(foodRow.portion_size_data)).asPortionSizeWithWeights, foodRow.reasonable_amount,
                          foodRow.food_group_id, foodRow.brand, fields, nutrients, customFieldsAsMap(foodRow.custom_fields))
                    }

                    val missingFoods = missingFoodRows.getOrElse(mealRow.meal_id, Seq()).map {
                      mfr =>
                        ExportMissingFood(mfr.mf_food_id, mfr.name, mfr.brand, mfr.description, mfr.portion_size, mfr.leftovers)
                    }

                    ExportMeal(mealRow.meal_id, mealRow.name, MealTime(mealRow.hours, mealRow.minutes), customFieldsAsMap(mealRow.custom_fields), foods, missingFoods)
                }
                ExportSubmission(submissionRow.id, submissionRow.user_id, submissionRow.user_name, customFieldsAsMap(submissionRow.user_custom_fields), customFieldsAsMap(submissionRow.submission_custom_fields),
                  submissionRow.start_time, submissionRow.end_time, submissionRow.submission_time, meals)
            }

            logger.debug(s"Create submission objects time: ${System.currentTimeMillis() - t6} ms")

            Right(submissions)
          }
        } else
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  def getSurveySubmissionCount(surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime): Either[LookupError, Int] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val surveyExists = SQL("SELECT 1 FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt).isDefined

        if (surveyExists) {

          val countQuery =
            """SELECT count(*) FROM survey_submissions AS ss
              |WHERE ss.survey_id={survey_id}
              |  AND ({time_from}::timestamp with time zone IS NULL OR start_time>{time_from})
              |  AND ({time_to}::timestamp with time zone IS NULL OR end_time<{time_to})
              |""".stripMargin

          Right(SQL(countQuery).on('survey_id -> surveyId, 'time_from -> dateFrom, 'time_to -> dateTo).executeQuery().as(SqlParser.int(1).single))

        } else
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))

      }
  }

  def createExportTask(userId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, purpose: String): Either[UnexpectedDatabaseError, Long] = tryWithConnection {
    implicit conn =>
      Right(SQL("INSERT INTO data_export_tasks(id, survey_id, date_from, date_to, user_id, created_at, purpose) VALUES(DEFAULT, {survey_id}, {date_from}, {date_to}, {user_id}, NOW(), {purpose})")
        .on('survey_id -> surveyId, 'date_from -> dateFrom, 'date_to -> dateTo, 'user_id -> userId, 'purpose -> purpose)
        .executeInsert(SqlParser.scalar[Long].single))
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

  def setExportTaskSuccess(taskId: Long): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      if (SQL("UPDATE data_export_tasks SET progress=1,successful=true,completed_at=NOW() WHERE id={task_id}")
        .on('task_id -> taskId)
        .executeUpdate() == 1)
        Right(())
      else
        Left(RecordNotFound(new RuntimeException(s"Export task id $taskId does not exist")))
  }

  def setExportTaskFailure(taskId: Long, cause: Throwable): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      if (SQL("UPDATE data_export_tasks SET successful=false,stack_trace={stack_trace},completed_at=NOW() WHERE id={id}")
        .on('id -> taskId, 'stack_trace -> collectStackTrace(cause).toArray)
        .executeUpdate() == 1)
        Right(())
      else Left(RecordNotFound(new RuntimeException(s"Export task id $taskId does not exist")))
  }


  private case class ExportTaskStatusDownloadRow(id: Long, created_at: ZonedDateTime, date_from: ZonedDateTime,
                                                 date_to: ZonedDateTime, progress: Option[Double], successful: Option[Boolean],
                                                 upload_successful: Option[Boolean], download_url: Option[String], download_url_expires_at: Option[ZonedDateTime])

  def getActiveExportTasks(surveyId: String, userId: Long): Either[LookupError, Seq[ExportTaskInfo]] = tryWithConnection {
    implicit conn =>
      val downloadTasks = SQL(
        """SELECT data_export_tasks.id, created_at, date_from, date_to, progress, successful, upload_successful, download_url, download_url_expires_at
          |FROM
          |  data_export_tasks LEFT JOIN data_export_downloads d3 ON data_export_tasks.id = d3.task_id
          |WHERE purpose='download' AND user_id={user_id} AND survey_id={survey_id}
          |  AND created_at > (now() - interval '1 day')
          |  AND (download_url_expires_at > now() OR download_url_expires_at IS NULL)
          |ORDER BY created_at DESC""".stripMargin)
        .on('user_id -> userId, 'survey_id -> surveyId)
        .as(Macro.namedParser[ExportTaskStatusDownloadRow].*)
        .map {
          row =>
            val status = row.successful match {
              case Some(true) => row.upload_successful match {
                case Some(true) => ExportTaskStatus.DownloadUrlAvailable(row.download_url.get)
                case Some(false) => ExportTaskStatus.Failed
                case None => ExportTaskStatus.DownloadUrlPending
              }
              case Some(false) => ExportTaskStatus.Failed
              case None =>
                row.progress match {
                  case Some(progress) => ExportTaskStatus.InProgress(progress)
                  case None => ExportTaskStatus.Pending
                }
            }

            ExportTaskInfo(row.id, row.created_at, row.date_from, row.date_to, status)
        }

      //TODO: add upload tasks if needed

      Right(downloadTasks)
  }

  def setExportTaskDownloadUrl(taskId: Long, url: URL, expiresAt: ZonedDateTime): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      SQL(
        """INSERT INTO data_export_downloads(task_id, upload_successful, download_url, download_url_expires_at)
          |VALUES ({task_id},true,{download_url},{download_url_expires_at})""".stripMargin)
        .on('task_id -> taskId, 'download_url -> url.toString, 'download_url_expires_at -> expiresAt)
        .execute()

      Right(())
  }

  def setExportTaskDownloadFailed(taskId: Long, cause: Throwable): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      SQL(
        """INSERT INTO data_export_downloads(task_id, upload_successful, stack_trace)
          |VALUES ({task_id}, false, {stack_trace})""".stripMargin)
        .on('task_id -> taskId, 'stack_trace -> cause.getMessage)
        .execute()

      Right(())
  }
}
