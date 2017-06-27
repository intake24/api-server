package akka.export

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, AllForOneStrategy, Props}
import controllers.system.asynchronous.ExportTask
import play.api.Logger
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.DataExportService

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer.MailerClient

object ExportTaskHandler {

  case class Complete(file: File)

  case class Failed(cause: Throwable)
}

class ExportTaskHandler(exportService: DataExportService, task: ExportTask, batchSize: Int, throttleRateMs: Int) extends Actor {

  override val supervisorStrategy = AllForOneStrategy() {
    case e: Throwable => {
      Logger.error("Shite, unexpected error!", e)
      context.stop(self)
      Stop
    }
  }

  Logger.info(s"ExportTaskHandler starting (${self.path})")

  val submissionLoader = context.actorOf(Props(classOf[SubmissionLoader], exportService, task.surveyId, task.dateFrom,
    task.dateTo, batchSize), "SubmissionLoader")

  val csvBuilder = context.actorOf(Props(classOf[CSVBuilder], task), "CSVBuilder")

  submissionLoader ! SubmissionLoader.GetNextSubmissionBatch

  def receive: Receive = {
    case SubmissionLoader.SubmissionBatch(submissions) =>
      Logger.info(s"Got next submission batch, size " + submissions.size)
      csvBuilder ! CSVBuilder.WriteNextBatch(submissions)

    case SubmissionLoader.DatabaseError =>
      Logger.info(s"Got database error :(((")
      Logger.info(s"Notifying user!")
      context.stop(self)

    case CSVBuilder.WriteComplete =>
      Logger.info(s"File write complete, scheduling next submission batch")
      context.system.scheduler.scheduleOnce(throttleRateMs milliseconds, submissionLoader, SubmissionLoader.GetNextSubmissionBatch)

    case SubmissionLoader.NoMoreSubmissions =>
      Logger.info(s"All submissions processed, closing file")
      csvBuilder ! CSVBuilder.Finalise

    case CSVBuilder.FileReady(file) =>
      Logger.info(s"File ready")
      context.parent ! ExportTaskHandler.Complete(file)
      context.stop(self)


    case SubmissionLoader.DatabaseError =>
  }
}
