package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.DataExportService
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}
import uk.ac.ncl.openlab.intake24.surveydata._

class DataExportImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends DataExportService with SqlDataService with SqlResourceLoader {

  lazy val getSurveySubmissionsSql = sqlFromResource("admin/get_survey_submissions.sql")

  lazy val getSurveySubmissionMealsSql = sqlFromResource("admin/get_survey_submission_meals.sql")

  lazy val getSurveySubmissionFoodsSql = sqlFromResource("admin/get_survey_submission_foods.sql")

  lazy val getSurveySubmissionNutrientsSql = sqlFromResource("admin/get_survey_submission_nutrients.sql")

  private case class SubmissionRow(id: UUID, survey_id: String, user_id: String, start_time: Instant, end_time: Instant, log: String,
                                   submission_custom_fields: Array[Array[String]], user_custom_fields: Array[Array[String]])

  private case class MealRow(submission_id: UUID, meal_id: Long, hours: Int, minutes: Int, name: String, custom_fields: Array[Array[String]])

  private case class FoodRow(meal_id: Long, food_id: Long, code: String, english_description: String, local_description: Option[String], ready_meal: Boolean, search_term: String,
                             portion_size_method_id: String, reasonable_amount: Boolean, food_group_id: Int, brand: String, nutrient_table_id: String, nutrient_table_code: String,
                             custom_fields: Array[Array[String]], portion_size_data: Array[Array[String]])

  private case class NutrientRow(food_id: Long, n_type: Long, n_amount: Double)

  private def customFieldsAsMap(fields: Array[Array[String]]) =
    fields.foldLeft(Map[String, String]()) {
      case (acc, cf) => acc + (cf(0) -> cf(1))
    }

  def getSurveySubmissions(surveyId: String, dateFrom: Option[Instant], dateTo: Option[Instant], offset: Int, limit: Int, respondentId: Option[String]): Either[LookupError, Seq[ExportSubmission]] = tryWithConnection {
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
                    ExportMeal(mealRow.name, MealTime(mealRow.hours, mealRow.minutes), customFieldsAsMap(mealRow.custom_fields), foods)
                }
                ExportSubmission(submissionRow.id, submissionRow.user_id, customFieldsAsMap(submissionRow.user_custom_fields), customFieldsAsMap(submissionRow.submission_custom_fields),
                  submissionRow.start_time, submissionRow.end_time, meals)
            }

            Right(submissions)
          }
        } else
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

}
