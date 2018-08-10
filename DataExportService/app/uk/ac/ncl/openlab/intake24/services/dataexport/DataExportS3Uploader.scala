package uk.ac.ncl.openlab.intake24.services.dataexport

import java.io.File
import java.net.URL
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import javax.inject.{Inject, Named, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.errors.{AnyError, ErrorUtils}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.DataExportService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataExportS3Uploader @Inject()(configuration: Configuration,
                                     dataExportService: DataExportService,
                                     s3Client: AmazonS3,
                                     @Named("intake24") implicit val executionContext: ExecutionContext) {

  val logger = LoggerFactory.getLogger(classOf[DataExportS3Uploader])

  val configSection = "intake24.asyncDataExporter"

  val bucketName = configuration.get[String](s"$configSection.s3.bucketName")
  val pathPrefix = configuration.get[String](s"$configSection.s3.pathPrefix")
  val urlExpirationTimeMinutes = configuration.get[Long](s"$configSection.s3.urlExpirationTimeMinutes")


  private def uploadImpl(fileName: String, file: File, expirationDate: Date): Either[AnyError, URL] =
    ErrorUtils.catchAll {
      val objName = s"$pathPrefix/$fileName"
      s3Client.putObject(bucketName, objName, file)
      s3Client.generatePresignedUrl(bucketName, objName, expirationDate, HttpMethod.GET)
    }

  def upload(task: ExportTaskHandle, s3FileName: String): Future[Either[AnyError, URL]] =
    task.result.map {
      fileResult =>
        (for (file <- fileResult;
              _ <- Right(logger.debug(s"[${task.id}] uploading ${file.getAbsolutePath} to S3"));
              urlExpirationDate <- Right(ZonedDateTime.now().plus(urlExpirationTimeMinutes, ChronoUnit.MINUTES));
              url <- uploadImpl(s3FileName, file, Date.from(urlExpirationDate.toInstant));
              _ <- dataExportService.setExportTaskDownloadUrl(task.id, url, urlExpirationDate))
          yield url).left.map {
          error =>
            logger.error(s"[${task.id}] upload to S3 failed", error.exception)
            dataExportService.setExportTaskDownloadFailed(task.id, error.exception).left.map {
              setError =>
                logger.warn(s"[${task.id}] could not set failed status for data export download", setError.exception)
            }
            error
        }
    }
}
