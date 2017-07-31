package controllers.system.asynchronous

import java.io.{File, FileWriter, IOException}
import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.{Date, UUID}
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import au.com.bytecode.opencsv.CSVWriter
import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client, AmazonS3ClientBuilder}
import controllers.system.asynchronous.ExportManager.TaskFinished
import org.slf4j.LoggerFactory
import parsers.SurveyCSVExporter
import play.api.libs.mailer.{Email, MailerClient}
import play.api.{Configuration, Logger}
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import views.html.DataExportNotification

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class ExportTask(taskId: Long, userName: Option[String], userEmail: Option[String], surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, dataScheme: CustomDataScheme,
                      foodGroups: Map[Int, FoodGroupRecord], localNutrients: Seq[LocalNutrientDescription], insertBOM: Boolean)


object ExportManager {

  case class QueueTask(task: ExportTask)

  case object TaskFinished

}

case class ExportManagerConfig(
                                batchSize: Int,
                                throttleRateMs: Int,
                                maxActiveTasks: Int,
                                s3BucketName: String,
                                s3PathPrefix: String,
                                s3UrlExpirationTimeMinutes: Long
                              )

class ExportManager(exportService: DataExportService, s3Client: AmazonS3, mailer: MailerClient, config: ExportManagerConfig) extends Actor {

  case class CSVFileHandles(file: File, fileWriter: FileWriter, csvWriter: CSVWriter)

  val logger = LoggerFactory.getLogger(classOf[ExportManager])

  val scheduler = new ThrottlingScheduler {
    def run(f: => Unit): Unit = context.system.scheduler.scheduleOnce(config.throttleRateMs milliseconds)(f)(play.api.libs.concurrent.Execution.defaultContext)
  }

  val queue = mutable.Queue[ExportTask]()


  var activeTasks = 0

  def dbSetStarted(taskId: Long): ThrottledTask[Unit] = ThrottledTask.fromAnyError {
    exportService.setExportTaskStarted(taskId)
  }

  def dbSetSuccessful(taskId: Long, downloadUrl: String): ThrottledTask[Unit] = ThrottledTask.fromAnyError {
    exportService.setExportTaskSuccess(taskId, downloadUrl)
  }

  def dbSetFailed(taskId: Long, cause: Throwable): ThrottledTask[Unit] = ThrottledTask.fromAnyError {
    exportService.setExportTaskFailure(taskId, cause)
  }

  def prepareFile(task: ExportTask): ThrottledTask[CSVFileHandles] = new ThrottledTask[CSVFileHandles] {
    def run(scheduler: ThrottlingScheduler)(onComplete: (Try[CSVFileHandles]) => Unit): Unit = {

      logger.debug(s"[${task.taskId}] creating a temporary CSV file for export")

      var file: File = null
      var fileWriter: FileWriter = null
      var csvWriter: CSVWriter = null

      try {

        file = SurveyCSVExporter.createTempFile()
        fileWriter = new FileWriter(file)
        csvWriter = new CSVWriter(fileWriter)

        logger.debug(s"[${task.taskId}] writing CSV header")

        SurveyCSVExporter.writeHeader(fileWriter, csvWriter, task.dataScheme, task.localNutrients, task.insertBOM)

        onComplete(Success(CSVFileHandles(file, fileWriter, csvWriter)))
      } catch {
        case e: IOException =>

          try {

            if (fileWriter != null)
              fileWriter.close()

            if (csvWriter != null)
              csvWriter.close()

            if (file != null)
              file.delete()
          } catch {
            case e: IOException =>
              logger.warn("Error when cleaning up CSV resources after prepareFile failure", e)
          }

          onComplete(Failure(e))
      }
    }
  }

  def tryWithHandles[T](handles: CSVFileHandles)(f: CSVFileHandles => T): Try[T] =
    try {
      Success(f(handles))
    } catch {
      case e: IOException =>
        try {
          handles.csvWriter.close()
          handles.fileWriter.close()
          handles.file.delete()
        } catch {
          case e2: IOException =>
            logger.warn("Error when cleaning up CSV resources after tryWithHandles failure", e)
        }

        Failure(e)
    }


  def closeFile(taskId: Long, handles: CSVFileHandles): ThrottledTask[Unit] = ThrottledTask.fromTry(tryWithHandles(handles) {
    logger.debug(s"[${taskId}] flushing and closing the CSV file")

    handles =>
      handles.csvWriter.close()
      handles.fileWriter.close()
  })

  def getTotalSubmissionCount(task: ExportTask): ThrottledTask[Int] = ThrottledTask.fromAnyError {
    exportService.getSurveySubmissionCount(task.surveyId, task.dateFrom, task.dateTo)
  }

  def exportNextBatch(task: ExportTask, handles: CSVFileHandles, offset: Int): ThrottledTask[Int] = ThrottledTask.fromTry({

    logger.debug(s"[${task.taskId}] exporting next submissions batch using offset $offset")

    exportService.getSurveySubmissions(task.surveyId, Some(task.dateFrom), Some(task.dateTo), offset, config.batchSize, None) match {
      case Right(submissions) if submissions.size > 0 =>
        tryWithHandles(handles) {
          handles =>
            SurveyCSVExporter.writeSubmissionsBatch(handles.csvWriter, task.dataScheme, task.foodGroups, task.localNutrients, submissions)
            submissions.size
        }
      case Right(_) => Success(0)
      case Left(error) => Failure(error.exception)
    }
  })

  def dbUpdateProgress(taskId: Long, progress: Double): ThrottledTask[Unit] = ThrottledTask.fromAnyError {

    logger.debug(s"[${taskId}] updating progress to $progress")

    exportService.updateExportTaskProgress(taskId, progress)
  }

  def exportRemaining(task: ExportTask, handles: CSVFileHandles, totalCount: Int, currentOffset: Int = 0): ThrottledTask[Unit] =
    if (totalCount == 0) {
      logger.debug(s"[${task.taskId}] expected total count is 0, skipping export steps")

      ThrottledTask {
        ()
      }
    }
    else {
      logger.debug(s"[${task.taskId}] expected total count is $totalCount")

      exportNextBatch(task, handles, currentOffset).flatMap {
        submissionsInLastBatch =>
          if (submissionsInLastBatch > 0) {
            val newOffset = currentOffset + config.batchSize
            val progress = (currentOffset + submissionsInLastBatch).toDouble / totalCount.toDouble

            for (_ <- dbUpdateProgress(task.taskId, progress);
                 _ <- exportRemaining(task, handles, totalCount, newOffset)) yield ()
          }
          else
            dbUpdateProgress(task.taskId, 1.0)
      }
    }

  def deleteFile(taskId: Long, handles: CSVFileHandles): ThrottledTask[Unit] = ThrottledTask {

    logger.debug(s"[${taskId}] deleting CSV file")

    handles.file.delete()
  }

  def uploadToS3(task: ExportTask, handles: CSVFileHandles): ThrottledTask[String] = ThrottledTask {

    logger.debug(s"[${task.taskId}] uploading CSV to S3")

    val dateFromString = DateTimeFormatter.ISO_DATE.format(task.dateFrom).replaceAll("Z", "")

    val dateToString = DateTimeFormatter.ISO_DATE.format(task.dateTo).replaceAll("Z", "")

    val fileName = s"${config.s3PathPrefix}/intake24-${task.surveyId}-data-${task.taskId}-$dateFromString-$dateToString.csv"

    s3Client.putObject(config.s3BucketName, fileName, handles.file)

    val expiration = new Date()
    //expiration.setTime(expiration.getTime + config.s3UrlExpirationTimeMinutes * 60 * 1000)
    s3Client.generatePresignedUrl(config.s3BucketName, fileName, expiration, HttpMethod.GET).toString
  }

  def notifySuccessful(task: ExportTask, downloadUrl: String): ThrottledTask[Unit] = ThrottledTask {


    task.userEmail match {
      case Some(email) =>

        val body = DataExportNotification(task.userName, task.surveyId, downloadUrl, config.s3UrlExpirationTimeMinutes.toInt / 60)

        val message = Email(s"Your Intake24 survey (${task.surveyId}) data is available for download", "Intake24 <support@intake24.co.uk>", Seq(email), None, Some(body.toString()))
        mailer.send(message)
      case None =>
        logger.warn(s"[${task.taskId}] exporting user has no e-mail address")
    }
  }

  def runExport(task: ExportTask): Unit = {

    val throttledTask = for (
      handles <- prepareFile(task);
      _ <- dbSetStarted(task.taskId);
      count <- getTotalSubmissionCount(task);
      _ <- exportRemaining(task, handles, count);
      _ <- closeFile(task.taskId, handles);
      url <- uploadToS3(task, handles);
      _ <- dbSetSuccessful(task.taskId, url);
      _ <- notifySuccessful(task, url);
      _ <- deleteFile(task.taskId, handles)
    ) yield url

    throttledTask.run(scheduler) {
      result =>
        result match {
          case Success(url) =>
            logger.debug(s"Export task ${task.taskId} successful, download URL is $url")
          case Failure(e) =>
            logger.error(s"Export task ${task.taskId} failed", e)
        }

        self ! TaskFinished
    }
  }

  def maybeStartNextTask() = {
    if (!queue.isEmpty && activeTasks < config.maxActiveTasks) {
      activeTasks += 1
      val task = queue.dequeue()

      runExport(task)

      logger.debug(s"Started task ${task.taskId}, current active tasks: " + activeTasks)
    }
  }

  def receive: Receive = {
    case ExportManager.QueueTask(task) =>
      queue += task
      logger.debug(s"Task ${task.taskId} queued, queue size: " + queue.size)
      maybeStartNextTask()

    case ExportManager.TaskFinished =>
      activeTasks -= 1
      maybeStartNextTask()
  }
}


object DataExporterCache {
  def progressKey(taskId: UUID) = s"DataExporter.$taskId.progress"

  def downloadUrlKey(taskId: UUID) = s"DataExporter.$taskId.url"
}

@Singleton
class AsynchronousDataExporter @Inject()(actorSystem: ActorSystem,
                                         configuration: Configuration,
                                         exportService: DataExportService,
                                         surveyAdminService: SurveyAdminService,
                                         mailer: MailerClient,
                                         s3Client: AmazonS3,
                                         userAdminService: UserAdminService,
                                         foodGroupsAdminService: FoodGroupsAdminService) {

  val logger = LoggerFactory.getLogger(classOf[AsynchronousDataExporter])

  val configSection = "intake24.asyncDataExporter"

  val exportManagerConfig = ExportManagerConfig(
    configuration.getInt(s"$configSection.batchSize").get,
    configuration.getInt(s"$configSection.throttleRateMs").get,
    configuration.getInt(s"$configSection.maxConcurrentTasks").get,
    configuration.getString(s"$configSection.s3.bucketName").get,
    configuration.getString(s"$configSection.s3.pathPrefix").get,
    configuration.getLong(s"$configSection.s3.urlExpirationTimeMinutes").get
  )

  val exportManager = actorSystem.actorOf(Props(classOf[ExportManager], exportService, s3Client, mailer, exportManagerConfig), "ExportManager")

  def unwrapAnyError[T](r: Either[AnyError, T]): Either[Throwable, T] = r.left.map(_.exception)

  def logFailureAndNotifyManager(taskId: Long, cause: Throwable, manager: ActorRef) = {
    exportService.setExportTaskFailure(taskId, cause) match {
      case Right(()) => manager
    }
  }

  def queueCsvExport(userId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, insertBOM: Boolean): Either[AnyError, Long] = {
    for (
      survey <- surveyAdminService.getSurveyParameters(surveyId).right;
      userProfile <- userAdminService.getUserById(userId).right;
      foodGroups <- foodGroupsAdminService.listFoodGroups(survey.localeId).right;
      dataScheme <- surveyAdminService.getCustomDataScheme(survey.schemeId).right;
      localNutrients <- surveyAdminService.getLocalNutrientTypes(survey.localeId).right;
      taskId <- exportService.createExportTask(ExportTaskParameters(userId, surveyId, dateFrom, dateTo)).right)
      yield {

        exportManager ! ExportManager.QueueTask(ExportTask(taskId, userProfile.name.map(_.split("\\s+").head), userProfile.email, surveyId, dateFrom, dateTo, dataScheme, foodGroups, localNutrients, insertBOM))

        taskId
      }
  }
}
