package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.time.{Instant, ZonedDateTime}
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.UserSurveyParameters
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

  def createSurvey(surveyId: String, parameters: NewSurveyParameters): Either[CreateError, SurveyParametersOut] = tryWithConnection {
    implicit conn =>
      tryWithConstraintCheck("surveys_id_pk", DuplicateCode(_)) {

        val sqlQuery =
          """
            |INSERT INTO surveys (
            |         id, state, start_date, end_date, scheme_id, locale,
            |         allow_gen_users, suspension_reason, survey_monkey_url, support_email
            |)
            |VALUES ({id}, 0, DEFAULT, DEFAULT,
            |        {scheme_id}, {locale}, {allow_gen_users}, '',
            |        {survey_monkey_url}, {support_email})
            |RETURNING id,
            |          state,
            |          start_date,
            |          end_date,
            |          scheme_id,
            |          locale,
            |          allow_gen_users,
            |          suspension_reason,
            |          survey_monkey_url,
            |          support_email
          """.stripMargin

        val row = SQL(sqlQuery)
          .on('id -> surveyId,
            'scheme_id -> parameters.schemeId,
            'locale -> parameters.localeId,
            'allow_gen_users -> parameters.allowGeneratedUsers,
            'survey_monkey_url -> parameters.externalFollowUpURL,
            'support_email -> parameters.supportEmail)
          .executeQuery().as(Macro.namedParser[SurveyParametersRow].single)
        Right(row.toSurveyParameters)
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
                                         allow_gen_users: Boolean, survey_monkey_url: Option[String], support_email: String) {

    def toSurveyParameters: SurveyParametersOut = new SurveyParametersOut(
      this.id, this.scheme_id, this.locale, this.state, this.start_date, this.end_date,
      this.suspension_reason, this.allow_gen_users, this.survey_monkey_url, this.support_email
    )

  }

  override def getSurveyParameters(surveyId: String): Either[LookupError, SurveyParametersOut] = tryWithConnection {
    implicit conn =>
      SQL("SELECT scheme_id, state, locale, start_date, end_date, suspension_reason, allow_gen_users, survey_monkey_url, support_email FROM surveys WHERE id={survey_id}")
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

  // TODO: Hard coded to match legacy behaviour, but needs better solution eventually
  override def getCustomDataScheme(schemeId: String): Either[LookupError, CustomDataScheme] = schemeId match {
    case "default" => Right(CustomDataScheme(Seq(), Seq(), Seq(), Seq()))
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
