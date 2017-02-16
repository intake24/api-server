package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.time.Instant
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class SurveyAdminImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends SurveyAdminService with SqlDataService with SqlResourceLoader {

  def createSurvey(surveyId: String, parameters: NewSurveyParameters): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      tryWithConstraintCheck("surveys_id_pk", DuplicateCode(_)) {
        SQL("INSERT INTO surveys VALUES ({id}, 0, DEFAULT, DEFAULT, {scheme_id}, {locale}, {allow_gen_users}, '', {survey_monkey_url}, {support_email})")
          .on('id -> surveyId, 'scheme_id -> parameters.schemeId, 'locale -> parameters.localeId, 'allow_gen_users -> parameters.allowGeneratedUsers,
            'survey_monkey_url -> parameters.externalFollowUpURL, 'support_email -> parameters.supportEmail)
          .execute()
        Right(())
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

  private case class SurveyParametersRow(scheme_id: String, state: Int, locale: String, start_date: Instant, end_date: Instant, suspension_reason: Option[String],
                                         allow_gen_users: Boolean, survey_monkey_url: Option[String], support_email: String)

  override def getSurveyParameters(surveyId: String): Either[LookupError, SurveyParameters] = tryWithConnection {
    implicit conn =>
      SQL("SELECT scheme_id, state, locale, start_date, end_date, suspension_reason, allow_gen_users, survey_monkey_url, support_email FROM surveys WHERE id={survey_id}")
        .executeQuery()
        .as(Macro.namedParser[SurveyParametersRow].singleOpt) match {
        case Some(row) =>
          Right(SurveyParameters(row.scheme_id, row.locale, row.state, row.start_date, row.end_date, row.suspension_reason, row.allow_gen_users, row.survey_monkey_url, row.support_email))
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
