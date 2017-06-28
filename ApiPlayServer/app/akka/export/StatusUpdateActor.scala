package akka.export

import akka.actor.Actor
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.DataExportService

import scala.concurrent.Future

import akka.pattern._

object StatusUpdateActor {

  case object SetStarted

  case class UpdateProgress(progress: Double)

  case class SetSuccessful(downloadUrl: String)

  case class SetFailed(cause: Throwable)


}

class StatusUpdateActor(exportService: DataExportService, taskId: Long) extends Actor {

  import akka.export.StatusUpdateActor._

  val logger = LoggerFactory.getLogger(classOf[StatusUpdateActor])

  def checkResult(result: Either[LookupError, Unit]) = result match {
    case Left(e) =>
      logger.error("Failed to update export task status", e.exception)
  }

  def receive: Receive = {
    case SetStarted =>
      Future {
        checkResult(exportService.setExportTaskStarted(taskId))
      }

    case UpdateProgress(progress) =>
      Future {
        checkResult(exportService.updateExportTaskProgress(taskId, progress))
      }

    case SetSuccessful(downloadUrl) =>
      Future {
        checkResult(exportService.setExportTaskSuccess(taskId, downloadUrl))
      }.map(_ => ExportTaskActor.StatusSet).pipeTo(sender())

    case SetFailed(cause: Throwable) =>
      Future {
        checkResult(exportService.setExportTaskFailure(taskId, cause))
        sender()
      }.map(_ => ExportTaskActor.StatusSet).pipeTo(sender())
  }
}
