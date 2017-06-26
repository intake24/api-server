package controllers.system.asynchronous

import java.io.{File, FileWriter, IOException}
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import au.com.bytecode.opencsv.CSVWriter
import controllers.system.asynchronous.AsynchronousDataExporter.ExportTask
import parsers.SurveyCSVExporter
import play.api.Logger
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.mailer.MailerClient
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService

import scala.collection.mutable
import scala.concurrent.duration._

object AsynchronousDataExporter {

  case object Tick

  case class ExportTask(taskId: UUID, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, dataScheme: CustomDataScheme,
                        foodGroups: Map[Int, FoodGroupRecord], localNutrients: Seq[LocalNutrientDescription], insertBOM: Boolean)

  class ExportManager(exportService: DataExportService, batchSize: Int, throttleRateMs: Int) extends Actor {

    Logger.info("ExportManager actor created")

    val queue = mutable.Queue[ExportTask]()

    var currentTask: ExportTask = null

    var currentOffset = 0

    var fileWriter: ActorRef = null


    def maybeBeginNextTask() = {
      if (!queue.isEmpty) {
        beginTask(queue.dequeue())
      } else {
        currentTask = null
        fileWriter = null
      }
    }

    def beginTask(task: ExportTask) = {

      currentTask = task

      currentOffset = 0

      fileWriter = context.system.actorOf(Props(classOf[CsvWriter], currentTask))

      self ! Tick
    }


    def receive: Receive = {

      case task: ExportTask =>

        if (currentTask == null) {
          Logger.info("No active task, starting immediately")
          beginTask(task)
        } else {
          Logger.info("Export task queued, queue length: " + (queue.size + 1))
          queue += task
        }

      case Tick =>


        exportService.getSurveySubmissions(currentTask.surveyId, Some(currentTask.dateFrom), Some(currentTask.dateTo), currentOffset, batchSize, None) match {
          case Right(submissions) => {

            Logger.info(s"Received next batch of ${submissions.size} submissions")

            if (submissions.size > 0) {
              currentOffset += submissions.size
              Logger.info(s"Writing ${submissions.size} submissions to file...")

              fileWriter ! WriteNextBatch(submissions)

              context.system.scheduler.scheduleOnce(throttleRateMs milliseconds, self, Tick)
            } else {
              Logger.info("All submissions for the curent task processed")

              fileWriter ! Finalise
            }
          }

          case Left(e) => {
            Logger.error("Error :(", e.exception)
          }
        }

      case FileReady(file) =>
        Logger.info("Upload, e-mail, bla bla bla")
        context.stop(fileWriter)
        maybeBeginNextTask()
    }
  }

}

class CsvWriter(exportTask: ExportTask) extends Actor {

  val outputFile = SurveyCSVExporter.createFile()

  val fileWriter = new FileWriter(outputFile)
  val csvWriter = new CSVWriter(fileWriter)

  SurveyCSVExporter.writeHeader(fileWriter, csvWriter, exportTask.dataScheme, exportTask.localNutrients, exportTask.insertBOM)

  def receive: Receive = {
    case WriteNextBatch(submissions) =>
      SurveyCSVExporter.writeSubmissionsBatch(csvWriter, exportTask.dataScheme, exportTask.foodGroups, exportTask.localNutrients, submissions)
    case Finalise =>
      fileWriter.close()
      csvWriter.close()
      sender() ! FileReady(outputFile)
  }

  override def postStop(): Unit = {
    fileWriter.close()
    csvWriter.close()
  }
}

//case class ReportSuccess(taskId: UUID)


case class WriteNextBatch(submissions: Seq[ExportSubmission])

case object Finalise

case class FileReady(file: File)

case object Complete


@Singleton
class AsynchronousDataExporter @Inject()(actorSystem: ActorSystem,
                                         exportService: DataExportService,
                                         surveyAdminService: SurveyAdminService,
                                         foodGroupsAdminService: FoodGroupsAdminService) {

  import AsynchronousDataExporter._

  val exportManager = actorSystem.actorOf(Props(classOf[ExportManager], exportService, 50, 50), "ExportManager")

  def queueCsvExport(surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, insertBOM: Boolean): Either[AnyError, UUID] = {
    for (
      survey <- surveyAdminService.getSurveyParameters(surveyId).right;
      foodGroups <- foodGroupsAdminService.listFoodGroups(survey.localeId).right;
      dataScheme <- surveyAdminService.getCustomDataScheme(survey.schemeId).right;
      localNutrients <- surveyAdminService.getLocalNutrientTypes(survey.localeId).right)
      yield {

        val taskId = UUID.randomUUID()

        exportManager ! ExportTask(taskId, surveyId, dateFrom, dateTo, dataScheme, foodGroups, localNutrients, insertBOM)

        taskId
      }
  }
}
