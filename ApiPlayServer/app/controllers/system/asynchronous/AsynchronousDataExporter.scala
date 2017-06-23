package controllers.system.asynchronous

import java.io.{File, FileWriter, IOException}
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import au.com.bytecode.opencsv.CSVWriter
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

    var currentTask: Option[ExportTask] = None

    var currentOffset = 0

    var file: File = null

    var fileWriter: FileWriter = null

    var csvWriter: CSVWriter = null


    def receive: Receive = {

      case task: ExportTask =>

        Logger.info("Export task queued: " + task + ", queue length: " + (queue.size + 1))

        queue += task
        self ! Tick

      case Tick =>

        currentTask match {
          case None =>
            if (queue.isEmpty) {
              Logger.info("Nothing left to do")
            } else {
              Logger.info("Starting next task from the queue")

              val task = queue.dequeue()

              currentTask = Some(task)

              file = SurveyCSVExporter.createFile()

              fileWriter = new FileWriter(file)

              csvWriter = new CSVWriter(fileWriter)

              SurveyCSVExporter.writeHeader(fileWriter, csvWriter, task.dataScheme, task.localNutrients, task.insertBOM)


              currentOffset = 0
              self ! Tick
            }

          case Some(task) =>
            exportService.getSurveySubmissions(task.surveyId, Some(task.dateFrom), Some(task.dateTo), currentOffset, batchSize, None) match {
              case Right(submissions) => {

                Logger.info(s"Received next batch of ${submissions.size} submissions")

                if (submissions.size > 0) {
                  currentOffset += submissions.size
                  Logger.info(s"Writing ${submissions.size} submissions to file...")



                  SurveyCSVExporter.writeSubmissionsBatch(csvWriter, task.dataScheme, task.foodGroups, task.localNutrients, submissions)

                  throw new IOException("Kotak :(")

                  context.system.scheduler.scheduleOnce(throttleRateMs milliseconds, self, Tick)
                } else {
                  Logger.info("All submissions for the curent task processed")
                  currentTask = None

                  fileWriter.close()
                  csvWriter.close()

                  Logger.info("Export complete: " + file.getAbsolutePath)

                  file = null
                  fileWriter = null
                  csvWriter = null

                  self ! Tick
                }
              }

              case Left(e) => {
                Logger.error("Error :(", e.exception)
              }
            }

        }
    }
  }

  class CsvWriter(dataScheme: CustomDataScheme, foodGroups: Map[Int, FoodGroupRecord], localNutrients: Seq[LocalNutrientDescription], insertBOM: Boolean) extends Actor {

    val outputFile = SurveyCSVExporter.createFile()
    outputFile.deleteOnExit()

    val fileWriter = new FileWriter(outputFile)
    val csvWriter = new CSVWriter(fileWriter)

    SurveyCSVExporter.writeHeader(fileWriter, csvWriter, dataScheme, localNutrients, insertBOM)

    def receive: Receive = {

      case WriteNextBatch(submissions) =>
        SurveyCSVExporter.writeSubmissionsBatch(csvWriter, dataScheme, foodGroups, localNutrients, submissions)

      case Complete =>
        fileWriter.close()
        csvWriter.close()

    }
  }

  //case class ReportSuccess(taskId: UUID)

  class NotificationManager(mailer: MailerClient) extends Actor {
    def receive: Receive = ???
  }


}


case class WriteNextBatch(submissions: Seq[ExportSubmission])

case object Finalise

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
