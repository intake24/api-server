package uk.ac.ncl.openlab.intake24.services.dataexport

import java.io.File
import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import io.circe.generic.auto._
import io.circe.parser.decode
import javax.inject.Named
import org.slf4j.LoggerFactory
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{PendingScheduledExportTask, ScheduledDataExportService}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class DataExportDaemon @Inject()(config: Configuration,
                                 dataExporter: SingleThreadedDataExporter,
                                 scheduledTasks: ScheduledDataExportService,
                                 ftpsUploader: DataExportFtpsUploader,
                                 system: ActorSystem,
                                 @Named("intake24") implicit val executionContext: ExecutionContext,
                                ) {

  private val logger = LoggerFactory.getLogger(classOf[DataExportDaemon])

  private val pollingPeriodSeconds = config.get[Int]("intake24.dataExport.scheduled.pollingIntervalSeconds")


  private def uploadFTPS(file: File, remoteName: String, config: String): Either[Throwable, Unit] =
    decode[FTPSConfig](config) match {
      case Right(config) => ftpsUploader.upload(file, remoteName, config)
      case Left(error) => Left(error)
    }

  private def runPostExportAction(task: PendingScheduledExportTask, exportTaskHandle: ExportTaskHandle): Future[Either[Throwable, Unit]] = {
    exportTaskHandle.result.map {
      case Right(file) =>
        task.action match {
          case "upload_ftps" =>
            val dateStamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.ofInstant(Clock.systemUTC().instant(), ZoneId.systemDefault).withNano(0)).replace(":", "").replace("T", "-")
            val remoteFileName = s"intake24-${task.surveyId}-data-${exportTaskHandle.id}-$dateStamp.csv"

            uploadFTPS(file, remoteFileName, task.actionConfig)
          case _ => Left(new RuntimeException(s"Unsupported export action: ${task.action}"))
        }
      case Left(error) =>
        Left(error.exception)
    }
  }

  system.scheduler.schedule(0.minutes, pollingPeriodSeconds.seconds) {

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

            val taskResult = for (exportTaskHandle <- EitherT(dataExporter.queueCsvExport(task.userId, task.surveyId, dateFrom, dateTo, true, "scheduled", "v1").map(_.left.map(_.exception)));
                                  _ <- EitherT(runPostExportAction(task, exportTaskHandle))) yield ()

            taskResult.value.onComplete {
              case Success(Right(())) => logger.debug(s"Post export action successfully executed for scheduled task ${task.id}")
              case Success(Left(e)) =>
                logger.error(s"Failed to apply post export action for scheduled task ${task.id}: ${task.action}", e)
              case Failure(e) =>
                logger.error(s"Failed to apply post export action for scheduled task ${task.id}: ${task.action}", e)
            }

            scheduledTasks.updateNextRunTime(task.id) match {
              case Right(()) => ()
              case Left(error) =>
                logger.error(s"Failed to update next run time for scheduled task ${task.id}", error.exception)
            }

        }

      case Left(error) => logger.error("Failed to get pending scheduled tasks", error.exception)
    }
  }
}
