package scheduled

import java.time.{ZoneId, ZoneOffset, ZonedDateTime}
import java.time.temporal.ChronoUnit
import javax.inject.Named

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import controllers.system.SurveyAdminController
import controllers.system.asynchronous.{ExportTaskHandle, SingleThreadedDataExporter}
import org.slf4j.LoggerFactory
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.ScheduledDataExportService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


@Singleton
class DataExportDaemon @Inject()(config: Configuration,
                                 dataExporter: SingleThreadedDataExporter,
                                 scheduledTasks: ScheduledDataExportService,
                                 system: ActorSystem,
                                 @Named("intake24") implicit val executionContext: ExecutionContext,
                                     ) {

  private val logger = LoggerFactory.getLogger(classOf[DataExportDaemon])

  private def runAction(scheduledTaskId: Long, action: String, actionConfig: String, taskHandle: ExportTaskHandle) = {

    taskHandle.result.map {
      case Right(file) =>
        logger.debug(s"Applying $action to ${file.getAbsolutePath}")
      case Left(error) =>
        logger.error(s"Export task ${taskHandle.id} initiated by scheduled task $scheduledTaskId failed", error)
    }
  }

  system.scheduler.schedule(0.minutes, 10.minutes) {

    scheduledTasks.getPendingScheduledTasks() match {
      case Right(tasks) =>
        logger.debug(s"${tasks.size} scheduled task(s) need(s) to be run")

        tasks.foreach {
          task =>

            val dateFrom = task.periodDays match {
              case Some(period) => ZonedDateTime.now().minus(period, ChronoUnit.DAYS)
              case None => ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            }

            val dateTo = ZonedDateTime.now()

            dataExporter.queueCsvExport(task.userId, task.surveyId, dateFrom, dateTo, true, "scheduled").map {
              case Right(taskHandle) => runAction(task.id, task.action, task.actionConfig, taskHandle)
              case Left(error) => logger.error(s"Failed to queue data export for scheduled task ${task.id}", error.exception)
            }

            scheduledTasks.updateNextRunTime(task.id) match {
              case Right(()) => ()
              case Left(error) =>
                logger.error(s"Failed to update next run time for scheduled task ${task.id}", error.exception)
            }

        }

      case Left(error) =>
        logger.error("Failed to get pending scheduled tasks", error.exception)
    }

  }
}
