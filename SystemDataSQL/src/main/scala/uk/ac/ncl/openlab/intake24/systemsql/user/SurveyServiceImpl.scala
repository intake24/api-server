package uk.ac.ncl.openlab.intake24.systemsql.user

import java.time.ZonedDateTime
import java.util.UUID

import javax.inject.{Inject, Named}
import javax.sql.DataSource
import anorm._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SurveyState
import uk.ac.ncl.openlab.intake24.services.systemdb.user._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}
import uk.ac.ncl.openlab.intake24.surveydata.NutrientMappedSubmission

class SurveyServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends SurveyService with SqlDataService with SqlResourceLoader {

  private case class UserSurveyParametersRow(id: String, scheme_id: String, state: Int, locale: String, started: Boolean, finished: Boolean, suspension_reason: Option[String],
                                             originating_url: Option[String], description: Option[String], store_user_session_on_server: Option[Boolean],
                                             number_of_submissions_for_feedback: Int)

  private case class UxEventSettingsRow(enable_search_events: Boolean, enable_associated_foods_events: Boolean)

  override def getPublicSurveyParameters(surveyId: String): Either[LookupError, PublicSurveyParameters] = tryWithConnection {
    implicit conn =>
      SQL("SELECT locale, respondent_language_id, support_email, originating_url FROM surveys JOIN locales ON locales.id = surveys.locale WHERE surveys.id={survey_id}")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as((SqlParser.str("locale") ~ SqlParser.str("respondent_language_id") ~ SqlParser.str("support_email") ~ SqlParser.str("originating_url").?).singleOpt) match {
        case Some(locale ~ respondentLanguageId ~ email ~ url) =>
          Right(PublicSurveyParameters(locale, respondentLanguageId, email, url))
        case None =>
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  override def getSurveyFeedbackStyle(surveyId: String): Either[LookupError, SurveyFeedbackStyle] = tryWithConnection {
    implicit conn =>
      val s = SQL("SELECT feedback_style FROM surveys WHERE surveys.id={survey_id};")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as(SqlParser.str("feedback_style").single)
      Right(SurveyFeedbackStyle(s))
  }

  override def getSurveyParameters(surveyId: String): Either[LookupError, UserSurveyParameters] = tryWithConnection {
    implicit conn =>
      SQL("SELECT id, scheme_id, locale, state, now() >= start_date AS started, now() > end_date AS finished, suspension_reason, originating_url, description, store_user_session_on_server, number_of_submissions_for_feedback FROM surveys WHERE id={survey_id}")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as(Macro.namedParser[UserSurveyParametersRow].singleOpt) match {
        case Some(row) => {

          val state: String = SurveyState.fromCode(row.state) match {
            case SurveyState.Active =>
              (row.started, row.finished) match {
                case (false, _) => "pending"
                case (_, true) => "finished"
                case _ => "running"
              }
            case SurveyState.Suspended =>
              "suspended"
            case SurveyState.NotInitialised =>
              "pending"
          }

          val uxEventsSettings = SQL("SELECT enable_search_events, enable_associated_foods_events FROM surveys_ux_events_settings WHERE survey_id={survey_id}")
            .on('survey_id -> surveyId)
            .executeQuery()
            .as(Macro.namedParser[UxEventSettingsRow].singleOpt) match {
            case Some(UxEventSettingsRow(search, foods)) => UxEventsSettings(search, foods)
            case None => UxEventsSettings(false, false)
          }

          Right(UserSurveyParameters(row.id, row.scheme_id, row.locale, state, row.suspension_reason, row.description, uxEventsSettings, row.store_user_session_on_server.getOrElse(false),
            row.number_of_submissions_for_feedback))

        }
        case None =>
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  private case class SurveyFollowUpRow(survey_monkey_url: Option[String], feedback_enabled: Boolean)

  override def getSurveyFollowUp(surveyId: String): Either[LookupError, SurveyFollowUp] = tryWithConnection {
    implicit conn =>
      SQL("SELECT survey_monkey_url, feedback_enabled FROM surveys WHERE id={survey_id}").on('survey_id -> surveyId).as(Macro.namedParser[SurveyFollowUpRow].singleOpt) match {
        case Some(row) => Right(SurveyFollowUp(row.survey_monkey_url, row.feedback_enabled))
        case None => Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  def createSubmission(userId: Long, surveyId: String, survey: NutrientMappedSubmission): Either[UnexpectedDatabaseError, UUID] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val generatedId = java.util.UUID.randomUUID()

        SQL("INSERT INTO survey_submissions VALUES ({id}::uuid, {survey_id}, {user_id}, {start_time}, {end_time}, ARRAY[{log}]::text[], {ux_session_id}::uuid)")
          .on('id -> generatedId, 'survey_id -> surveyId, 'user_id -> userId, 'start_time -> survey.startTime,
            'end_time -> survey.endTime, 'log -> Seq[String](), 'ux_session_id -> survey.uxSessionId)
          .execute()

        val customFieldParams = survey.customData.map {
          case (name, value) => Seq[NamedParameter]('survey_submission_id -> generatedId, 'name -> name, 'value -> value)
        }.toSeq

        if (!customFieldParams.isEmpty) {
          BatchSql("INSERT INTO survey_submission_custom_fields VALUES (DEFAULT, {survey_submission_id}::uuid, {name}, {value})", customFieldParams.head, customFieldParams.tail: _*).execute()
        }

        // Meals

        if (!survey.meals.isEmpty) {

          val mealParams = survey.meals.map {
            meal =>
              Seq[NamedParameter]('survey_submission_id -> generatedId, 'hours -> meal.time.hours, 'minutes -> meal.time.minutes, 'name -> meal.name)
          }

          val batch = BatchSql("INSERT INTO survey_submission_meals VALUES (DEFAULT, {survey_submission_id}::uuid, {hours}, {minutes}, {name})", mealParams.head, mealParams.tail: _*)

          val mealIds = AnormUtil.batchKeys(batch)

          val meals = mealIds.zip(survey.meals)

          // Custom fields

          val mealCustomFieldParams = meals.flatMap {
            case (meal_id, meal) =>
              meal.customData.map {
                case (name, value) => Seq[NamedParameter]('meal_id -> meal_id, 'name -> name, 'value -> value)
              }
          }

          if (!mealCustomFieldParams.isEmpty) {
            BatchSql("INSERT INTO survey_submission_meal_custom_fields VALUES (DEFAULT, {meal_id}, {name}, {value})", mealCustomFieldParams.head, mealCustomFieldParams.tail: _*).execute()
          }

          // Foods

          val mealFoodsParams = meals.flatMap {
            case (meal_id, meal) =>
              meal.foods.map {
                case food =>
                  Seq[NamedParameter]('meal_id -> meal_id, 'code -> food.code, 'english_description -> food.englishDescription, 'local_description -> food.localDescription, 'ready_meal -> food.isReadyMeal, 'search_term -> food.searchTerm,
                    'portion_size_method_id -> food.portionSize.method, 'reasonable_amount -> food.reasonableAmount, 'food_group_id -> food.foodGroupId, 'food_group_english_description -> food.foodGroupEnglishDescription,
                    'food_group_local_description -> food.foodGroupLocalDescription, 'brand -> food.brand, 'nutrient_table_id -> food.nutrientTableId, 'nutrient_table_code -> food.nutrientTableCode)
              }
          }

          if (!mealFoodsParams.isEmpty) {

            val batch = BatchSql("INSERT INTO survey_submission_foods (id, meal_id, code, english_description, local_description, ready_meal, search_term, portion_size_method_id, reasonable_amount, food_group_id, food_group_english_description, food_group_local_description, brand, nutrient_table_id, nutrient_table_code) VALUES (DEFAULT, {meal_id}, {code}, {english_description}, {local_description}, {ready_meal}, {search_term}, {portion_size_method_id}, {reasonable_amount},{food_group_id},{food_group_english_description},{food_group_local_description},{brand},{nutrient_table_id},{nutrient_table_code})",
              mealFoodsParams.head, mealFoodsParams.tail: _*)

            val foodIds = AnormUtil.batchKeys(batch)

            val foods = foodIds.zip(meals.flatMap(_._2.foods))

            // Food custom fields

            val foodCustomFieldParams = foods.flatMap {
              case (food_id, food) =>
                food.customData.map {
                  case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
                }
            }

            if (!foodCustomFieldParams.isEmpty) {
              BatchSql("INSERT INTO survey_submission_food_custom_fields VALUES (DEFAULT, {food_id}, {name}, {value})", foodCustomFieldParams.head, foodCustomFieldParams.tail: _*).execute()
            }

            // Food portion size method parameters

            val foodPortionSizeMethodParams = foods.flatMap {
              case (food_id, food) =>
                food.portionSize.data.map {
                  case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
                }
            }

            if (!foodPortionSizeMethodParams.isEmpty) {
              BatchSql("INSERT INTO survey_submission_portion_size_fields VALUES (DEFAULT, {food_id}, {name}, {value})", foodPortionSizeMethodParams.head, foodPortionSizeMethodParams.tail: _*).execute()
            }

            // Food nutrient values

            val foodNutrientParams = foods.flatMap {
              case (food_id, food) =>
                food.nutrients.map {
                  case (nutrientTypeId, value) => Seq[NamedParameter]('food_id -> food_id, 'nutrient_type_id -> nutrientTypeId, 'value -> value)
                }
            }

            if (!foodNutrientParams.isEmpty) {
              BatchSql("INSERT INTO survey_submission_nutrients(id, food_id, nutrient_type_id, amount) VALUES (DEFAULT, {food_id}, {nutrient_type_id}, {value})", foodNutrientParams.head, foodNutrientParams.tail: _*).execute()
            }
          }

          // Missing foods

          val missingFoodsParams = meals.flatMap {
            case (meal_id, meal) =>
              meal.missingFoods.map {
                case missingFood =>
                  Seq[NamedParameter]('meal_id -> meal_id, 'name -> missingFood.name, 'brand -> missingFood.brand, 'description -> missingFood.description,
                    'portion_size -> missingFood.portionSize, 'leftovers -> missingFood.leftovers)
              }
          }

          if (missingFoodsParams.nonEmpty) {
            BatchSql("INSERT INTO survey_submission_missing_foods VALUES(DEFAULT,{meal_id},{name},{brand},{description},{portion_size},{leftovers})", missingFoodsParams.head, missingFoodsParams.tail: _*).execute()
          }
        }

        Right(generatedId)
      }
  }

  override def userSubmittedWithinPeriod(surveyId: String, userId: Long, dateFrom: ZonedDateTime, dateTo: ZonedDateTime): Either[UnexpectedDatabaseError, Boolean] = tryWithConnection {
    implicit conn =>
      val r = SQL(
        """
          |SELECT EXISTS(
          |    SELECT 1
          |    FROM survey_submissions
          |    WHERE survey_id = {survey_id} AND
          |          user_id = {user_id} AND
          |          end_time >= {date_from} AND
          |          end_time <= {date_to}
          |)
        """.stripMargin)
        .on('survey_id -> surveyId, 'user_id -> userId, 'date_from -> dateFrom, 'date_to -> dateTo).executeQuery().as(SqlParser.bool("exists").single)
      Right(r)
  }

  override def getNumberOfSubmissionsForUser(surveyId: String, userId: Long): Either[UnexpectedDatabaseError, Int] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT COUNT(*) FROM survey_submissions WHERE survey_id={survey_id} AND user_id={user_id}").on('survey_id -> surveyId, 'user_id -> userId).executeQuery().as(SqlParser.int(1).single))
  }
}
