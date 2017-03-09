package uk.ac.ncl.openlab.intake24.systemsql.user

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SurveyState
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{PublicSurveyParameters, SurveyService, UserSurveyParameters}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class SurveyServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends SurveyService with SqlDataService with SqlResourceLoader {

  private case class UserSurveyParametersRow(scheme_id: String, state: Int, locale: String, started: Boolean, finished: Boolean, suspension_reason: Option[String], survey_monkey_url: Option[String], support_email: String)

  override def getPublicSurveyParameters(surveyId: String): Either[LookupError, PublicSurveyParameters] = tryWithConnection {
    implicit conn =>
      SQL("SELECT locale, support_email FROM surveys WHERE id={survey_id}")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as((SqlParser.str("locale") ~ SqlParser.str("support_email")).singleOpt ) match {
        case Some(locale ~ email) =>
          Right(PublicSurveyParameters(locale, email))
        case None =>
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  override def getSurveyParameters(surveyId: String): Either[LookupError, UserSurveyParameters] = tryWithConnection {
    implicit conn =>
      SQL("SELECT scheme_id, state, locale, now() >= start_date AS started, now() > end_date AS finished, suspension_reason, survey_monkey_url, support_email FROM surveys WHERE id={survey_id}")
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

          Right(UserSurveyParameters(row.scheme_id, row.locale, state, row.suspension_reason, row.survey_monkey_url, row.support_email))
        }
      }
  }
}
