package akka.export

import java.time.ZonedDateTime

import akka.actor.Actor
import akka.pattern._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import uk.ac.ncl.openlab.intake24.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, ExportSubmission}

import scala.concurrent.Future

object SubmissionLoader {

  case object GetNextSubmissionBatch

  case object NoMoreSubmissions

  case class SubmissionBatch(submissions: Seq[ExportSubmission])

  case class DatabaseError(error: LookupError)

}

class SubmissionLoader(exportService: DataExportService, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, batchSize: Int) extends Actor {

  import SubmissionLoader._

  Logger.info(s"SubmissionLoader starting (${self.path})")

  var offset = 0

  def receive: Receive = {
    case GetNextSubmissionBatch =>
      Future {
        exportService.getSurveySubmissions(surveyId, Some(dateFrom), Some(dateTo), offset, batchSize, None) match {
          case Right(submissions) =>
            if (submissions.size > 0) {
              offset += submissions.size
              SubmissionBatch(submissions)
            }
            else
              NoMoreSubmissions
          case Left(error) =>
            DatabaseError(error)
        }
      }.pipeTo(sender())
  }
}
