package uk.ac.ncl.openlab.intake24.systemsql.user

import java.time.Instant
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{GWTClientErrorReport, GWTClientErrorService, NewGWTClientErrorReport}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class GWTClientErrorServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends GWTClientErrorService with SqlDataService with SqlResourceLoader {
  def submitErrorReport(report: NewGWTClientErrorReport): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("INSERT INTO gwt_client_error_reports VALUES(DEFAULT,{user_id},{survey_id},{reported_at},{gwt_permutation_name},{exception_chain_json},{survey_state_json},true)")
        .on('user_id -> report.userId,
          'survey_id -> report.surveyId,
          'reported_at -> report.reportedAt,
          'gwt_permutation_name -> report.gwtPermutationName,
          'exception_chain_json -> report.exceptionChainJSON,
          'survey_state_json -> report.surveyStateJSON)
        .execute()
      Right(())
  }

  private case class GWTClientErrorReportRow(id: Long, user_id: Option[String], survey_id: Option[String], reported_at: Instant, gwt_permutation_name: String, exception_chain_json: String, survey_state_json: String) {
    def toGWTClientErrorReport = GWTClientErrorReport(id, user_id, survey_id, reported_at, gwt_permutation_name, exception_chain_json, survey_state_json)
  }

  def getNewErrorReports(): Either[UnexpectedDatabaseError, Seq[GWTClientErrorReport]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT id, user_id, survey_id, reported_at, gwt_permutation_name, exception_chain_json, survey_state_json FROM gwt_client_error_reports WHERE new")
        .as(Macro.namedParser[GWTClientErrorReportRow].*)
        .map(_.toGWTClientErrorReport))
  }

  def markAsSeen(ids: Seq[Long]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      Right(
        SQL("UPDATE gwt_client_error_reports SET new=false WHERE id IN({ids})")
          .on('ids -> ids)
          .execute())
  }
}
