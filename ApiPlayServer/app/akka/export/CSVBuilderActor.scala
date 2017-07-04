package akka.export

import java.io.{File, FileWriter}

import akka.actor.Actor
import akka.pattern._
import au.com.bytecode.opencsv.CSVWriter
import controllers.system.asynchronous.ExportTask
import parsers.SurveyCSVExporter
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.ExportSubmission

import scala.concurrent.Future

object CSVBuilderActor {

  case class WriteNextBatch(submissions: Seq[ExportSubmission])

  case object WriteComplete

  case object Finalise

  case class FileReady(file: File)

}

class CSVBuilderActor(exportTask: ExportTask) extends Actor {

  import CSVBuilderActor._

  Logger.info(s"CSVBuilder starting (${self.path})")

  throw new RuntimeException("BAM! Can't create the file")

  val outputFile = SurveyCSVExporter.createTempFile()

  val fileWriter = new FileWriter(outputFile)
  val csvWriter = new CSVWriter(fileWriter)

  SurveyCSVExporter.writeHeader(fileWriter, csvWriter, exportTask.dataScheme, exportTask.localNutrients, exportTask.insertBOM)

  def receive: Receive = {
    case WriteNextBatch(submissions) =>

      Future {
        SurveyCSVExporter.writeSubmissionsBatch(csvWriter, exportTask.dataScheme, exportTask.foodGroups, exportTask.localNutrients, submissions)
      }.map(_ => WriteComplete).pipeTo(sender())

    case Finalise =>

      Future {
        fileWriter.close()
        csvWriter.close()
      }.map(_ => FileReady(outputFile)).pipeTo(sender())
  }

  override def postStop(): Unit = {
    Logger.info("CSVBuilder kirdik")
    fileWriter.close()
    csvWriter.close()
  }
}
