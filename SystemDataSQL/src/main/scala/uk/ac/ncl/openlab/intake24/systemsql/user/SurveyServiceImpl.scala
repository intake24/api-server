package uk.ac.ncl.openlab.intake24.systemsql.user

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{PublicSurveyParameters, SurveyService}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class SurveyServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends SurveyService with SqlDataService with SqlResourceLoader {

  private case class PublicSurveyParametersRow(scheme_id: String, state: Int, locale: String, in_progress: Boolean, suspension_reason: Option[String], support_email: String)

  override def getPublicSurveyParameters(surveyId: String): Either[LookupError, PublicSurveyParameters] = tryWithConnection {
    implicit conn =>
      SQL("SELECT locale FROM surveys WHERE id={survey_id}")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as(SqlParser.str("locale").singleOpt) match {
        case Some(locale) =>
          Right(PublicSurveyParameters(locale))
        case None =>
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }
}
