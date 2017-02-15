package uk.ac.ncl.openlab.intake24.systemsql.admin

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, SecureUserRecord, SurveyAdminService}
import uk.ac.ncl.openlab.intake24.services.systemdb.errors._
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.systemsql.SystemSqlService

class SurveyAdminImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends SurveyAdminService with SystemSqlService with SqlResourceLoader {

  def createSurvey(surveyId: String, schemeId: String, localeId: String, allowGeneratedUsers: Boolean, externalFollowUpUrl: Option[String], supportEmail: String): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      tryWithConstraintCheck("surveys_id_pk", DuplicateCode(_)) {
        SQL("INSERT INTO surveys VALUES ({id}, 0, DEFAULT, DEFAULT, {scheme_id}, {locale}, {allow_gen_users}, '', {survey_monkey_url}, {support_email})")
          .on('id -> surveyId, 'scheme_id -> schemeId, 'locale -> localeId, 'allow_gen_users -> allowGeneratedUsers, 'survey_monkey_url -> externalFollowUpUrl,
            'support_email -> supportEmail)
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
}
