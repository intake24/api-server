package controllers.system.asynchronous

import java.io.{File, FileWriter, IOException}
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.export.ExportTaskActor
import au.com.bytecode.opencsv.CSVWriter
import org.slf4j.LoggerFactory
import parsers.SurveyCSVExporter
import play.api.Logger
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._

import scala.collection.mutable
import scala.concurrent.duration._

import resource.managed

case class ExportTask(taskId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, dataScheme: CustomDataScheme,
                      foodGroups: Map[Int, FoodGroupRecord], localNutrients: Seq[LocalNutrientDescription], insertBOM: Boolean)


object ExportManager {

  case class QueueTask(task: ExportTask)

  case object TaskFinished

}

class ExportManager(exportService: DataExportService, batchSize: Int, throttleRateMs: Int, maxActiveTasks: Int) extends Actor {

  val queue = mutable.Queue[ExportTask]()

  var activeTasks = 0

  def maybeStartNextTask() = {
    if (!queue.isEmpty && activeTasks < maxActiveTasks) {
      activeTasks += 1
      val task = queue.dequeue()
      context.actorOf(Props(classOf[ExportTaskActor], exportService, task, batchSize, throttleRateMs))
      Logger.info("Started task, active tasks: " + activeTasks)
    }
  }

  def receive: Receive = {
    case ExportManager.QueueTask(task) =>
      queue += task
      Logger.info("Task queued, queue size: " + queue.size)
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
                                         exportService: DataExportService,
                                         cache: DataExporterCache,
                                         surveyAdminService: SurveyAdminService,
                                         foodGroupsAdminService: FoodGroupsAdminService) {

  val logger = LoggerFactory.getLogger(classOf[AsynchronousDataExporter])

  def unwrapAnyError[T](r: Either[AnyError, T]): Either[Throwable, T] = r.left.map(_.exception)

  case class CSVFileHandles(file: File, fileWriter: FileWriter, csvWriter: CSVWriter)

  def prepareFile(task: ExportTask): Either[Throwable, CSVFileHandles] = {

    var file: File = null
    var fileWriter: FileWriter = null
    var csvWriter: CSVWriter = null

    try {

      file = SurveyCSVExporter.createTempFile()
      fileWriter = new FileWriter(file)
      csvWriter = new CSVWriter(fileWriter)

      SurveyCSVExporter.writeHeader(fileWriter, csvWriter, task.dataScheme, task.localNutrients, task.insertBOM)

      Right(CSVFileHandles(file, fileWriter, csvWriter))
    } catch {
      case e: IOException => {

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

        Left(e)
      }
    }
  }

  def tryWithHandles[T](handles: CSVFileHandles, f: CSVFileHandles => Either[Throwable, T]) = {
    val result = f(handles)

    if (result.isLeft) {
      try {
        handles.fileWriter.close()
        handles.csvWriter.close()
        handles.file.delete()
      } catch {
        case e: IOException =>
          logger.warn("Error when cleaning up CSV resources after failure", e)
      }
    }

    result
  }


  def logFailureAndNotifyManager(taskId: Long, cause: Throwable, manager: ActorRef) = {
    exportService.setExportTaskFailure(taskId, cause) match {
      case Right(()) => manager
    }
  }


  def finaliseFile(handles: CSVFileHandles): Either[Throwable, File] =
    try {

      handles.csvWriter.close()
      handles.fileWriter.close()

      Right(handles.file)
    } catch {
      case e: IOException => Left(e)
    }

  def writeBatch(submissions: Seq[ExportSubmission], task: ExportTask, csvWriter: CSVWriter): Either[Throwable, Unit] = {
    try {
      SurveyCSVExporter.writeSubmissionsBatch(csvWriter, task.dataScheme, task.foodGroups, task.localNutrients, submissions)
      Right(())
    } catch {
      case e: IOException => Left(e)
    }
  }

  def exportNextBatch(task: ExportTask, handles: CSVFileHandles, manager: ActorRef, offset: Int = 0): Unit = {

    tryWithHandles(handles) {
      handles =>

        exportService.getSurveySubmissions(task.surveyId, Some(task.dateFrom), Some(task.dateTo), offset, 50, None) match {
          case Right(submissions) if submissions.size > 0 =>
            writeBatch(submissions, task, handles.csvWriter)
          case Right(_) => SurveyCSVExporter
            .
        }

    }

    def beginExport(task: ExportTask, manager: ActorRef): Unit = {
      prepareFile(task.taskId) match {
        case Right(handles) =>
          exportService.setExportTaskStarted()

        case Left(e) => logFailureAndNotifyManager(task.taskId, e, manager)
      }


      (for (handles <- prepareFile(task.taskId).right;
            _ <- unwrapAnyError(exportService.setExportTaskStarted(task.taskId)).right)
        yield handles)
      match {
        case Right(handles) => actorSystem.scheduler.scheduleOnce(10 milliseconds) {
          exportNextBatch(task, handles, manager)
        }
        case Left(e) => actorSystem.scheduler.scheduleOnce(10 milliseconds) {
          logFailureAndNotifyManager(task.taskId, e, manager)
        }
      }
    }


    def queueCsvExport(userId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, insertBOM: Boolean): Either[AnyError, UUID] = {
      for (
        survey <- surveyAdminService.getSurveyParameters(surveyId).right;
        foodGroups <- foodGroupsAdminService.listFoodGroups(survey.localeId).right;
        dataScheme <- surveyAdminService.getCustomDataScheme(survey.schemeId).right;
        localNutrients <- surveyAdminService.getLocalNutrientTypes(survey.localeId).right;
        taskId <- exportService.createExportTask(ExportTaskParameters(userId, surveyId, dateFrom, dateTo)).right)
        yield {

          actorSystem.scheduler.scheduleOnce(0 milliseconds) {

          }

          taskId
        }
    }
  }
