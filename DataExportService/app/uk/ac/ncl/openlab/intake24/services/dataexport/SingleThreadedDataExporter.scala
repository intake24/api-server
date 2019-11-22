package uk.ac.ncl.openlab.intake24.services.dataexport

import java.io._
import java.nio.charset.{Charset, StandardCharsets}
import java.time.ZonedDateTime

import au.com.bytecode.opencsv.CSVWriter
import javax.inject.{Inject, Named, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.errors.{AnyError, DatabaseError, UnexpectedException}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._

import scala.concurrent.{ExecutionContext, Future}

case class ExportTaskHandle(id: Long, result: Future[Either[AnyError, File]])

@Singleton
class SingleThreadedDataExporter @Inject()(configuration: Configuration,
                                           exportService: DataExportService,
                                           surveyAdminService: SurveyAdminService,
                                           userAdminService: UserAdminService,
                                           csvExportFormats: Map[String, SurveyCSVExporter],
                                           @Named("intake24") implicit private val executionContext: ExecutionContext,
                                           foodGroupsAdminService: FoodGroupsAdminService) {

  private val logger = LoggerFactory.getLogger(classOf[SingleThreadedDataExporter])

  private val configSection = "intake24.dataExport"

  private val throttleDelay = configuration.get[Int](s"$configSection.task.throttleRateMs")
  private val batchSize = configuration.get[Int](s"$configSection.task.batchSize")

  private def export(taskId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, dataScheme: CustomDataScheme,
                     foodGroups: Map[Int, FoodGroupRecord], localFields: Seq[LocalFieldDescription],
                     localNutrients: Seq[LocalNutrientDescription], insertBOM: Boolean, format: String): Future[Either[AnyError, File]] = {

    def throttle() =
      if (throttleDelay > 0)
        Thread.sleep(throttleDelay)

    @throws[IOException]
    def exportNextBatch(exporter: SurveyCSVExporter, csvWriter: CSVWriter, offset: Int): Either[DatabaseError, Int] = {
      logger.debug(s"[$taskId] exporting next submissions batch using offset $offset")

      exportService.getSurveySubmissions(surveyId, Some(dateFrom), Some(dateTo), offset, batchSize, None).map {
        submissions =>
          if (submissions.size > 0)
            exporter.writeSubmissionsBatch(csvWriter, dataScheme, foodGroups, localFields, localNutrients, submissions)
          submissions.size
      }
    }

    @throws[IOException]
    def exportRemaining(exporter: SurveyCSVExporter, csvWriter: CSVWriter, totalCount: Int, currentOffset: Int = 0): Either[DatabaseError, Unit] =
      if (totalCount == 0) {
        logger.debug(s"[$taskId] expected total count is 0, skipping export steps")
        Right(())
      }
      else {
        logger.debug(s"[$taskId] expected total count is $totalCount")

        exportNextBatch(exporter, csvWriter, currentOffset).flatMap {
          submissionsInLastBatch =>
            if (submissionsInLastBatch > 0) {
              val newOffset = currentOffset + batchSize
              val progress = (currentOffset + submissionsInLastBatch).toDouble / totalCount.toDouble

              exportService.updateExportTaskProgress(taskId, progress).map {
                _ =>
                  throttle()
                  exportRemaining(exporter, csvWriter, totalCount, newOffset)
              }
            }
            else
              exportService.updateExportTaskProgress(taskId, 1.0)
        }
      }

    Future {
      logger.debug(s"[$taskId] started export task")

      var file: File = null
      var fileWriter: OutputStreamWriter = null
      var csvWriter: CSVWriter = null

      try {

        csvExportFormats.get(format) match {
          case Some(exporter) =>
            logger.debug(s"[$taskId] creating a temporary CSV file for export")

            file = exporter.createTempFile()
            fileWriter = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)
            csvWriter = new CSVWriter(fileWriter)

            logger.debug(s"[$taskId] writing CSV header")

            exporter.writeHeader(fileWriter, csvWriter, dataScheme, localFields, localNutrients, insertBOM)

            throttle()

            for (_ <- exportService.setExportTaskStarted(taskId);
                 expectedCount <- exportService.getSurveySubmissionCount(surveyId, dateFrom, dateTo);
                 _ <- exportRemaining(exporter, csvWriter, expectedCount);
                 _ <- Right(logger.debug(s"[$taskId] setting successful state"));
                 _ <- exportService.setExportTaskSuccess(taskId)) yield {

              logger.debug(s"[$taskId] file: ${file.getAbsolutePath}")
              file
            }

          case None =>
            throw new IOException(s"Unsupported CSV export format: $format")
        }

      } catch {
        case e: IOException =>
          exportService.setExportTaskFailure(taskId, e).left.map {
            dbError =>
              logger.warn("Could not set CSV export task result to failed after an IOException due to a database error", dbError.exception)
          }

          try {
            if (file != null) {
              file.delete()
            }
          } catch {
            case e: SecurityException =>
              logger.warn("Could not delete the temporary file after an IOException during CSV export", e)
          }

          Left(UnexpectedException(e))
      } finally {
        try {
          if (fileWriter != null) {
            fileWriter.close()
          }

          if (csvWriter != null) {
            csvWriter.close()
          }
        } catch {
          case e: IOException =>
            logger.warn("Could not close the FileWriter or CSVWriter after CSV export", e)
        }
      }
    }
  }

  def queueCsvExport(userId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, insertBOM: Boolean, purpose: String, format: String): Future[Either[DatabaseError, ExportTaskHandle]] = {
    Future {
      for (
        survey <- surveyAdminService.getSurveyParameters(surveyId);
        foodGroups <- foodGroupsAdminService.listFoodGroups(survey.localeId);
        dataScheme <- surveyAdminService.getCustomDataScheme(survey.schemeId);
        localFields <- surveyAdminService.getLocalFields(survey.localeId);
        localNutrients <- surveyAdminService.getLocalNutrientTypes(survey.localeId);
        taskId <- exportService.createExportTask(userId, surveyId, dateFrom, dateTo, purpose)
      ) yield {
        ExportTaskHandle(taskId, export(taskId, surveyId, dateFrom, dateTo, dataScheme, foodGroups, localFields, localNutrients, insertBOM, format))
      }
    }
  }
}
