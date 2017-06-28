package akka.export

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.export.ExportTaskActor.TaskComplete
import controllers.system.asynchronous.ExportTask
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.DataExportService

import scala.concurrent.duration._

object ExportTaskActor {

  case object StatusSet

  case object TaskComplete

  case class Failed(cause: Throwable)

}

class ExportTaskActor(exportService: DataExportService, task: ExportTask, batchSize: Int, throttleRateMs: Int) extends Actor {

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Throwable => {

      Logger.error("Shite, unexpected error!", e)

      if (sender() == statusUpdater) {
        context.parent ! TaskComplete
        context.stop(self)
      }
      else
        statusUpdater ! StatusUpdateActor.SetFailed(e)

      Stop
    }
  }

  Logger.info(s"ExportTaskHandler starting (${self.path})")

  val submissionLoader = context.actorOf(Props(classOf[SubmissionLoaderActor], exportService, task.surveyId, task.dateFrom,
    task.dateTo, batchSize), "SubmissionLoader")

  val csvBuilder = context.actorOf(Props(classOf[CSVBuilderActor], task), "CSVBuilder")

  val statusUpdater = context.actorOf(Props(classOf[StatusUpdateActor], task.taskId), "StatusUpdate")

  statusUpdater ! StatusUpdateActor.SetStarted

  submissionLoader ! SubmissionLoaderActor.GetNextSubmissionBatch

  def receive: Receive = {
    case SubmissionLoaderActor.SubmissionBatch(submissions) =>
      Logger.info(s"Got next submission batch, size " + submissions.size)
      csvBuilder ! CSVBuilderActor.WriteNextBatch(submissions)

    case CSVBuilderActor.WriteComplete =>
      Logger.info(s"File write complete, scheduling next submission batch")
      context.system.scheduler.scheduleOnce(throttleRateMs milliseconds, submissionLoader, SubmissionLoaderActor.GetNextSubmissionBatch)

    case SubmissionLoaderActor.NoMoreSubmissions =>
      Logger.info(s"All submissions processed, closing file")
      csvBuilder ! CSVBuilderActor.Finalise

    case CSVBuilderActor.FileReady(file) =>
      Logger.info(s"File ready")
      statusUpdater ! StatusUpdateActor.SetSuccessful("blah blah")

    case SubmissionLoaderActor.DatabaseError(e) =>
      statusUpdater ! StatusUpdateActor.SetFailed(e.exception)

    case ExportTaskActor.StatusSet =>
      context.parent ! TaskComplete
      context.stop(self)
  }
}
