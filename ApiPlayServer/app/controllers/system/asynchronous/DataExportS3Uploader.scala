package controllers.system.asynchronous

import java.io.File
import java.net.URL
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
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
        for (file <- fileResult;
             _ <- Right(logger.debug(s"[${task.id}] uploading ${file.getAbsolutePath} to S3"));
             urlExpirationDate <- Right(Date.from(ZonedDateTime.now().plus(urlExpirationTimeMinutes, ChronoUnit.MINUTES).toInstant));
             url <- uploadImpl(s3FileName, file, urlExpirationDate);
             dataExportService.setExportTaskDownloadUrl(url))
          yield url
    }
}
