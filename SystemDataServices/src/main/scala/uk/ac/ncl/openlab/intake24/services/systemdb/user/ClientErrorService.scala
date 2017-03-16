package uk.ac.ncl.openlab.intake24.services.systemdb.user

import java.time.Instant

import uk.ac.ncl.openlab.intake24.errors._

case class NewClientErrorReport(userId: Option[String], surveyId: Option[String], reportedAt: Instant, stackTrace: Seq[String], surveyStateJSON: String)

case class ClientErrorReport(id: Long, userId: Option[String], surveyId: Option[String], reportedAt: Instant, stackTrace: Seq[String], surveyStateJSON: String)

trait ClientErrorService {

  def submitErrorReport(report: NewClientErrorReport): Either[UnexpectedDatabaseError, Unit]

  def getNewErrorReports(): Either[UnexpectedDatabaseError, Seq[ClientErrorReport]]

  def markAsSeen(ids: Seq[Long]): Either[UnexpectedDatabaseError, Unit]
}
