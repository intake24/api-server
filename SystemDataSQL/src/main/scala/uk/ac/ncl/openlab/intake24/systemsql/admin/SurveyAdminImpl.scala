package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.time.{Instant, ZonedDateTime}

import anorm.Macro.ColumnNaming
import javax.inject.{Inject, Named}
import javax.sql.DataSource
import anorm._
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{ErrorReportingSettings, UserSurveyParameters}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class SurveyAdminImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends SurveyAdminService with SqlDataService with SqlResourceLoader {

  def listSurveys(): Either[UnexpectedDatabaseError, Seq[SurveyParametersOut]] = tryWithConnection {
    implicit connection =>
      val sqlQuery =
        """
          |SELECT * FROM surveys;
        """.stripMargin

      val result = SQL(sqlQuery).executeQuery().as(Macro.namedParser[SurveyParametersRow].*).map(row => row.toSurveyParameters)

      Right(result)
  }

  def getSurvey(surveyId: String): Either[LookupError, SurveyParametersOut] = tryWithConnection {
    implicit connection =>
      val sqlQuery =
        """
          |SELECT * FROM surveys
          |WHERE id = {id};
        """.stripMargin

      SQL(sqlQuery).on('id -> surveyId).executeQuery().as(Macro.namedParser[SurveyParametersRow].singleOpt) match {
        case Some(r) => Right(r.toSurveyParameters)
        case None => Left(RecordNotFound(new RuntimeException(s"Survey with id: $surveyId not found")))
      }

  }

  def createSurvey(parameters: SurveyParametersIn): Either[CreateError, SurveyParametersOut] = tryWithConnection {
    implicit conn =>

      val errors = Map[String, PSQLException => CreateError](
        "surveys_id_pk" -> (e => DuplicateCode(e)),
        "surveys_id_characters" -> (e => ConstraintViolation("survey_id_characters", new RuntimeException("Survey ID contains invalid characters")))
      )

      tryWithConstraintsCheck(errors) {
        val sqlQuery =
          """
            |INSERT INTO surveys (
            |         id, state, start_date, end_date,
            |         scheme_id, locale, allow_gen_users, gen_user_key, suspension_reason,
            |         survey_monkey_url, support_email, description,
            |         final_page_html,
            |         submission_notification_url,
            |         feedback_enabled, number_of_submissions_for_feedback,
            |         store_user_session_on_server, maximum_daily_submissions,
            |         maximum_total_submissions, minimum_submission_interval,
            |         auth_url_domain_override, client_error_report_state,
            |         client_error_report_stack_trace
            |)
            |VALUES ({id}, {state}, {start_date}, {end_date},
            |        {scheme_id}, {locale}, {allow_gen_users}, {gen_user_key}, '',
            |        {survey_monkey_url}, {support_email}, {description},
            |        {final_page_html},
            |        {submission_notification_url},
            |        {feedback_enabled}, {number_of_submissions_for_feedback},
            |        {store_user_session_on_server}, {maximum_daily_submissions},
            |        {maximum_total_submissions}, {minimum_submission_interval},
            |        {auth_url_domain_override}, {client_error_report_state},
            |        {client_error_report_stack_trace})
            |RETURNING id,
            |          state,
            |          start_date,
            |          end_date,
            |          scheme_id,
            |          locale,
            |          allow_gen_users,
            |          gen_user_key,
            |          suspension_reason,
            |          survey_monkey_url,
            |          support_email,
            |          description,
            |          final_page_html,
            |          submission_notification_url,
            |          feedback_enabled,
            |          number_of_submissions_for_feedback,
            |          store_user_session_on_server,
            |          maximum_daily_submissions,
            |          maximum_total_submissions,
            |          minimum_submission_interval,
            |          auth_url_domain_override,
            |          client_error_report_state,
            |          client_error_report_stack_trace;
          """.stripMargin

        val row = SQL(sqlQuery)
          .on('id -> parameters.id,
            'state -> parameters.state,
            'start_date -> parameters.startDate,
            'end_date -> parameters.endDate,
            'scheme_id -> parameters.schemeId,
            'locale -> parameters.localeId,
            'allow_gen_users -> parameters.allowGeneratedUsers,
            'gen_user_key -> parameters.generateUserKey,
            'survey_monkey_url -> parameters.externalFollowUpURL,
            'support_email -> parameters.supportEmail,
            'description -> parameters.description,
            'final_page_html -> parameters.finalPageHtml,
            'submission_notification_url -> parameters.submissionNotificationUrl,
            'feedback_enabled -> parameters.feedbackEnabled,
            'number_of_submissions_for_feedback -> parameters.numberOfSubmissionsForFeedback,
            'store_user_session_on_server -> parameters.storeUserSessionOnServer,
            'maximum_daily_submissions -> parameters.maximumDailySubmissions,
            'maximum_total_submissions -> parameters.maximumTotalSubmissions,
            'minimum_submission_interval -> parameters.minimumSubmissionInterval,
            'auth_url_domain_override -> parameters.authUrlDomainOverride,
            'client_error_report_state -> parameters.errorReporting.reportSurveyState,
            'client_error_report_stack_trace -> parameters.errorReporting.reportStackTrace)
          .executeQuery().as(Macro.namedParser[SurveyParametersRow].single)
        Right(row.toSurveyParameters)
      }
  }

  def updateSurvey(surveyId: String, parameters: SurveyParametersIn): Either[UpdateError, SurveyParametersOut] = tryWithConnection {
    implicit conn =>

      val errors = Map[String, PSQLException => UpdateError](
        "surveys_id_pk" -> (e => DuplicateCode(e)),
        "surveys_id_characters" -> (e => ConstraintViolation("survey_id_characters", new RuntimeException("Survey ID contains invalid characters"))),
        "surveys_maximum_daily_submissions_at_least_one" ->
          (_ => ConstraintViolation("surveys_maximum_daily_submissions_at_least_one", new RuntimeException("Maximum daily submissions must not be less than one")))
      )

      tryWithConstraintsCheck(errors) {
        val sqlQuery =
          """
            |UPDATE surveys
            |SET id={id},
            |    state={state},
            |    start_date={start_date},
            |    end_date={end_date},
            |    scheme_id={scheme_id},
            |    locale={locale},
            |    allow_gen_users={allow_gen_users},
            |    gen_user_key={gen_user_key},
            |    survey_monkey_url={survey_monkey_url},
            |    support_email={support_email},
            |    description={description},
            |    final_page_html={final_page_html},
            |    submission_notification_url={submission_notification_url},
            |    feedback_enabled={feedback_enabled},
            |    number_of_submissions_for_feedback={number_of_submissions_for_feedback},
            |    store_user_session_on_server={store_user_session_on_server},
            |    maximum_daily_submissions={maximum_daily_submissions},
            |    maximum_total_submissions={maximum_total_submissions},
            |    minimum_submission_interval={minimum_submission_interval},
            |    auth_url_domain_override={auth_url_domain_override},
            |    client_error_report_state={client_error_report_state},
            |    client_error_report_stack_trace={client_error_report_stack_trace}
            |WHERE id={survey_id}
            |RETURNING id,
            |          state,
            |          start_date,
            |          end_date,
            |          scheme_id,
            |          locale,
            |          allow_gen_users,
            |          gen_user_key,
            |          suspension_reason,
            |          survey_monkey_url,
            |          support_email,
            |          description,
            |          final_page_html,
            |          submission_notification_url,
            |          feedback_enabled,
            |          number_of_submissions_for_feedback,
            |          store_user_session_on_server,
            |          maximum_daily_submissions,
            |          maximum_total_submissions,
            |          minimum_submission_interval,
            |          auth_url_domain_override,
            |          client_error_report_state,
            |          client_error_report_stack_trace;
          """.stripMargin

        val row = SQL(sqlQuery)
          .on('survey_id -> surveyId,
            'id -> parameters.id,
            'state -> parameters.state,
            'start_date -> parameters.startDate,
            'end_date -> parameters.endDate,
            'scheme_id -> parameters.schemeId,
            'locale -> parameters.localeId,
            'allow_gen_users -> parameters.allowGeneratedUsers,
            'gen_user_key -> parameters.generateUserKey,
            'survey_monkey_url -> parameters.externalFollowUpURL,
            'support_email -> parameters.supportEmail,
            'description -> parameters.description,
            'final_page_html -> parameters.finalPageHtml,
            'submission_notification_url -> parameters.submissionNotificationUrl,
            'feedback_enabled -> parameters.feedbackEnabled,
            'number_of_submissions_for_feedback -> parameters.numberOfSubmissionsForFeedback,
            'store_user_session_on_server -> parameters.storeUserSessionOnServer,
            'maximum_daily_submissions -> parameters.maximumDailySubmissions,
            'maximum_total_submissions -> parameters.maximumTotalSubmissions,
            'minimum_submission_interval -> parameters.minimumSubmissionInterval,
            'auth_url_domain_override -> parameters.authUrlDomainOverride,
            'client_error_report_state -> parameters.errorReporting.reportSurveyState,
            'client_error_report_stack_trace -> parameters.errorReporting.reportStackTrace)
          .executeQuery().as(Macro.namedParser[SurveyParametersRow].single)
        Right(row.toSurveyParameters)
      }
  }

  def staffUpdateSurvey(surveyId: String, parameters: StaffSurveyUpdate): Either[UpdateError, SurveyParametersOut] = tryWithConnection {
    implicit conn =>
      val sqlQuery =
        """
          |UPDATE surveys
          |SET start_date={start_date},
          |    end_date={end_date},
          |    state={state},
          |    survey_monkey_url={survey_monkey_url},
          |    support_email={support_email},
          |    description={description},
          |    final_page_html={final_page_html}
          |WHERE id={survey_id}
          |RETURNING id,
          |          state,
          |          start_date,
          |          end_date,
          |          scheme_id,
          |          locale,
          |          allow_gen_users,
          |          gen_user_key,
          |          suspension_reason,
          |          survey_monkey_url,
          |          support_email,
          |          description,
          |          final_page_html,
          |          submission_notification_url,
          |          feedback_enabled,
          |          number_of_submissions_for_feedback,
          |          store_user_session_on_server,
          |          maximum_daily_submissions,
          |          maximum_total_submissions,
          |          minimum_submission_interval,
          |          auth_url_domain_override,
          |          client_error_report_state,
          |          client_error_report_stack_trace;
        """.stripMargin

      val row = SQL(sqlQuery)
        .on('survey_id -> surveyId,
          'start_date -> parameters.startDate,
          'end_date -> parameters.endDate,
          'state -> parameters.state,
          'survey_monkey_url -> parameters.externalFollowUpURL,
          'support_email -> parameters.supportEmail,
          'description -> parameters.description,
          'final_page_html -> parameters.finalPageHtml)
        .executeQuery().as(Macro.namedParser[SurveyParametersRow].single)
      Right(row.toSurveyParameters)
  }

  def validateSurveyId(surveyId: String): Either[CreateError, Unit] =
    if (!surveyId.matches("^[A-Za-z0-9_-]+$"))
      Left(ConstraintViolation("surveys_id_characters", new RuntimeException("Survey ID is empty or contains invalid characters")))
    else
      tryWithConnection {
        implicit conn =>

          SQL("SELECT 1 FROM surveys WHERE id={surveyId}").on('surveyId -> surveyId).executeQuery().as(SqlParser.long(1).singleOpt) match {
            case Some(_) => Left(DuplicateCode(new RuntimeException(s"Survey ID $surveyId already exists")))
            case None => Right(())
          }
      }

  def deleteSurvey(surveyId: String): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      tryWithConstraintCheck[DeleteError, Unit]("users_surveys_id_fk", e => StillReferenced(e)) {
        if (SQL("DELETE FROM surveys WHERE id = {survey_id}").on('survey_id -> surveyId).executeUpdate() == 1)
          Right(())
        else
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exit")))
      }
  }

  private case class SurveyParametersRow(id: String, scheme_id: String, state: Int, locale: String,
                                         start_date: ZonedDateTime, end_date: ZonedDateTime, suspension_reason: Option[String],
                                         allow_gen_users: Boolean, gen_user_key: Option[String], survey_monkey_url: Option[String], support_email: String,
                                         description: Option[String], final_page_html: Option[String],
                                         submission_notification_url: Option[String],
                                         feedback_enabled: Boolean, number_of_submissions_for_feedback: Int,
                                         store_user_session_on_server: Option[Boolean],
                                         maximum_daily_submissions: Int,
                                         maximum_total_submissions: Option[Int],
                                         minimum_submission_interval: Int,
                                         auth_url_domain_override: Option[String],
                                         client_error_report_state: Boolean,
                                         client_error_report_stack_trace: Boolean) {

    def toSurveyParameters: SurveyParametersOut = new SurveyParametersOut(
      this.id, this.scheme_id, this.locale, this.state, this.start_date, this.end_date,
      this.suspension_reason, this.allow_gen_users, this.gen_user_key, this.survey_monkey_url, this.support_email,
      this.description, this.final_page_html, this.submission_notification_url, this.feedback_enabled,
      this.number_of_submissions_for_feedback, this.store_user_session_on_server, this.maximum_daily_submissions,
      this.maximum_total_submissions, this.minimum_submission_interval, this.auth_url_domain_override,
      ErrorReportingSettings(client_error_report_state, this.client_error_report_stack_trace)
    )
  }

  override def getSurveyParameters(surveyId: String): Either[LookupError, SurveyParametersOut] = tryWithConnection {
    implicit conn =>
      SQL(
        """SELECT id, scheme_id, state, locale, start_date, end_date, suspension_reason,
          |allow_gen_users, gen_user_key, survey_monkey_url, support_email, description, final_page_html, submission_notification_url,
          |feedback_enabled, number_of_submissions_for_feedback, store_user_session_on_server, maximum_daily_submissions,
          |maximum_total_submissions, minimum_submission_interval, auth_url_domain_override, client_error_report_state,
          |client_error_report_stack_trace FROM surveys WHERE id={survey_id}""".stripMargin)
        .on('survey_id -> surveyId)
        .executeQuery()
        .as(Macro.namedParser[SurveyParametersRow].singleOpt) match {
        case Some(row) =>
          Right(row.toSurveyParameters)
        case None =>
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  private case class LocalNutrientTypeRow(nutrient_type_id: Int, description: String, symbol: String) {
    def toLocalNutrientDescription = LocalNutrientDescription(nutrient_type_id, description, symbol)
  }

  private lazy val localNutrientTypesQuery = sqlFromResource("get_local_nutrient_types.sql")

  def getLocalNutrientTypes(localeId: String): Either[LookupError, Seq[LocalNutrientDescription]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT 1 FROM locales WHERE id={locale_id}").on('locale_id -> localeId).executeQuery().as(SqlParser.long(1).singleOpt) match {
          case Some(_) => Right(SQL(localNutrientTypesQuery).on('locale_id -> localeId).executeQuery().as(Macro.namedParser[LocalNutrientTypeRow].*).map(_.toLocalNutrientDescription))
          case None => Left(RecordNotFound(new RuntimeException(s"Locale $localeId does not exist")))
        }
      }
  }

  def getLocalFields(localeId: String): Either[LookupError, Seq[LocalFieldDescription]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("SELECT 1 FROM locales WHERE id={locale_id}").on('locale_id -> localeId).executeQuery().as(SqlParser.long(1).singleOpt) match {
          case Some(_) => {
            Right(SQL("select field_name, description from local_fields where locale_id={locale_id} order by id")
              .on('locale_id -> localeId).executeQuery()
              .as(Macro.namedParser[LocalFieldDescription](ColumnNaming.SnakeCase).*))
          }
          case None => Left(RecordNotFound(new RuntimeException(s"Locale $localeId does not exist")))
        }
      }
  }

  // TODO: Hard coded to match legacy behaviour, but needs better solution eventually
  override def getCustomDataScheme(schemeId: String): Either[LookupError, CustomDataScheme] = schemeId match {
    case "default" => Right(CustomDataScheme(Seq(), Seq(), Seq(), Seq()))
    case "bham1119" => Right(CustomDataScheme(Seq(), Seq(), Seq(
      CustomFieldDescription("foodSources", "Food sources"),
      CustomFieldDescription("mealLocation", "Meal location")
    ), Seq()))
    case "ndns419" => Right(CustomDataScheme(Seq(), Seq(
      CustomFieldDescription("cookingOil", "Cooking oil used"),
      CustomFieldDescription("diet", "Diet"),
      CustomFieldDescription("supplements", "Supplements"),
      CustomFieldDescription("foodAmount", "Food amount"),
      CustomFieldDescription("foodAmountReason", "Reason for unusual food amount")),
      Seq(
        CustomFieldDescription("foodSource", "Food source")),
      Seq(
        CustomFieldDescription("servingWeightFactor", "As served weight factor")
      )))
    case "ndns_default" | "ndns1019" => Right(CustomDataScheme(Seq(), Seq(
      CustomFieldDescription("cookingOil", "Cooking oil used"),
      CustomFieldDescription("diet", "Diet"),
      CustomFieldDescription("foodAmount", "Food amount"),
      CustomFieldDescription("foodAmountReason", "Reason for unusual food amount"),
      CustomFieldDescription("proxy", "Proxy"),
      CustomFieldDescription("proxyIssues", "Proxy Issues")),
      Seq(
        CustomFieldDescription("foodSource", "Food source")),
      Seq(
        CustomFieldDescription("servingWeightFactor", "As served weight factor")
      )))
    case "ndns_follow_up" => Right(CustomDataScheme(Seq(), Seq(
      CustomFieldDescription("cookingOil", "Cooking oil used"),
      CustomFieldDescription("diet", "Diet"),
      CustomFieldDescription("foodAmount", "Food amount"),
      CustomFieldDescription("foodAmountReason", "Reason for unusual food amount"),
      CustomFieldDescription("proxy", "Proxy"),
      CustomFieldDescription("proxyIssues", "Proxy Issues"),
      CustomFieldDescription("selfIsolation", "Self Isolation"),
      CustomFieldDescription("infrequentFood_selectedFish", "InfrequentFood SelectedFish"),
      CustomFieldDescription("infrequentFood_anyFish", "InfrequentFood AnyFish"),
      CustomFieldDescription("infrequentFood_whiteMeat", "InfrequentFood WhiteMeat"),
      CustomFieldDescription("infrequentFood_fruitJuice", "InfrequentFood FruitJuice"),
      CustomFieldDescription("infrequentFood_softDrinks", "InfrequentFood SoftDrinks")),
      Seq(
        CustomFieldDescription("foodSource", "Food source")),
      Seq(
        CustomFieldDescription("servingWeightFactor", "As served weight factor")
      )))
    case "sab" => Right(CustomDataScheme(
      Seq(
        CustomFieldDescription("interviewerId", "Interviewer ID"),
        CustomFieldDescription("interviewerName", "Interviewer Name"),
        CustomFieldDescription("interviewerTeamName", "Interviewer Team Name"),
        CustomFieldDescription("country", "Country")
      ),
      Seq(
        CustomFieldDescription("cookingOil", "Cooking oil used"),
        CustomFieldDescription("diet", "Diet"),
        CustomFieldDescription("supplements", "Supplements"),
        CustomFieldDescription("foodAmount", "Food amount"),
        CustomFieldDescription("foodAmountReason", "Reason for unusual food amount"),
        CustomFieldDescription("diffParticipant", "Participant difficulties"),
        CustomFieldDescription("diffInterviewer", "Interviewer difficulties")),
      Seq(
        CustomFieldDescription("foodSource", "Food source")),
      Seq(
        CustomFieldDescription("servingWeightFactor", "As served weight factor")
      )))
    case "experimental-pa-rules" => Right(CustomDataScheme(Seq(), Seq(), Seq(), Seq()))
    case "experimental-flexible-recall" => Right(CustomDataScheme(Seq(), Seq(), Seq(), Seq()))
    case "young_scot_2014" =>
      Right(CustomDataScheme(
        Seq(CustomFieldDescription("age", "Age"),
          CustomFieldDescription("gender", "Gender"),
          CustomFieldDescription("postCode", "Post code"),
          CustomFieldDescription("schoolName", "School"),
          CustomFieldDescription("townName", "Town")),
        Seq(CustomFieldDescription("lunchSpend", "Avg. lunch spending"),
          CustomFieldDescription("shopFreq", "Shop frequency"),
          CustomFieldDescription("packFreq", "Packed frequency"),
          CustomFieldDescription("schoolLunchFreq", "School frequency"),
          CustomFieldDescription("homeFreq", "Home/friend frequency"),
          CustomFieldDescription("skipFreq", "Skip frequency"),
          CustomFieldDescription("workFreq", "Work through frequency"),
          CustomFieldDescription("reason", "Reason"),
          CustomFieldDescription("freeMeals", "Free school meals")),
        Seq(CustomFieldDescription("mealLocation", "Meal location"),
          CustomFieldDescription("shopSpending", "Spending at shop/restaurant"),
          CustomFieldDescription("schoolSpending", "Spending at school")),
        Seq(CustomFieldDescription("foodSource", "Food source"))))
    case "shes_jun_2015" =>
      Right(CustomDataScheme(
        Seq(),
        Seq(CustomFieldDescription("dayOfWeek", "Day of week"),
          CustomFieldDescription("usualFoods", "Usual foods"),
          CustomFieldDescription("foodAmount", "Food amount"),
          CustomFieldDescription("supplements", "Supplements"),
          CustomFieldDescription("diet", "Diet")),
        Seq(),
        Seq()
      ))
    case "crowdflower_nov_2015" =>
      Right(CustomDataScheme(
        Seq(),
        Seq(CustomFieldDescription("external-user-id", "Crowdflower ID")),
        Seq(),
        Seq()
      ))
    case _ => Left(RecordNotFound(new RuntimeException(s"Unknown survey scheme name: $schemeId")))
  }

}
