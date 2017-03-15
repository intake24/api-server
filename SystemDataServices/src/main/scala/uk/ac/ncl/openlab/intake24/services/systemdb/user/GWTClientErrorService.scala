package uk.ac.ncl.openlab.intake24.services.systemdb.user

import java.time.Instant

import uk.ac.ncl.openlab.intake24.errors._

case class NewGWTClientErrorReport(userId: Option[String], surveyId: Option[String], reportedAt: Instant, gwtPermutationName: String, exceptionChainJSON: String, surveyStateJSON: String)

case class GWTClientErrorReport(id: Long, userId: Option[String], surveyId: Option[String], reportedAt: Instant, gwtPermutationName: String, exceptionChainJSON: String, surveyStateJSON: String)

trait GWTClientErrorService {

  def submitErrorReport(report: NewGWTClientErrorReport): Either[UnexpectedDatabaseError, Unit]

  def getNewErrorReports(): Either[UnexpectedDatabaseError, Seq[GWTClientErrorReport]]

  def markAsSeen(ids: Seq[Long]): Either[UnexpectedDatabaseError, Unit]
}
