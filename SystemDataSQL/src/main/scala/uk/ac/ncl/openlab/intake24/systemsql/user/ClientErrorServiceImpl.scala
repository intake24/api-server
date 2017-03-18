package uk.ac.ncl.openlab.intake24.systemsql.user

import java.time.Instant
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{ClientErrorReport, ClientErrorService, NewClientErrorReport}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class ClientErrorServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends ClientErrorService with SqlDataService with SqlResourceLoader {
  def submitErrorReport(report: NewClientErrorReport): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      SQL("INSERT INTO client_error_reports VALUES(DEFAULT,{user_id},{survey_id},{reported_at},ARRAY[{stack_trace}],{survey_state_json},true)")
        .on('user_id -> report.userId,
          'survey_id -> report.surveyId,
          'reported_at -> report.reportedAt,
          'stack_trace -> report.stackTrace,
          'survey_state_json -> report.surveyStateJSON)
        .execute()
      Right(())
  }

  private case class ClientErrorReportRow(id: Long, user_id: Option[String], survey_id: Option[String], reported_at: Instant, stack_trace: Array[String], survey_state_json: String) {
    def toClientErrorReport = ClientErrorReport(id, user_id, survey_id, reported_at, stack_trace, survey_state_json)
  }

  def getNewErrorReports(): Either[UnexpectedDatabaseError, Seq[ClientErrorReport]] = tryWithConnection {
    implicit conn =>
      Right(SQL("SELECT id, user_id, survey_id, reported_at, stack_trace, survey_state_json FROM client_error_reports WHERE new")
        .as(Macro.namedParser[ClientErrorReportRow].*)
        .map(_.toClientErrorReport))
  }

  def markAsSeen(ids: Seq[Long]): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      Right(
        SQL("UPDATE client_error_reports SET new=false WHERE id IN({ids})")
          .on('ids -> ids)
          .execute())
  }
}
