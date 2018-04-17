package controllers.system.asynchronous

import java.net.URL
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Named, Singleton}

import com.amazonaws.services.s3.AmazonS3
import org.slf4j.LoggerFactory
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, UserAdminService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataExportS3Uploader @Inject()(configuration: Configuration,
                                     dataExportService: DataExportService,
                                     s3Client: AmazonS3,
                                     @Named("long-tasks") implicit val executionContext: ExecutionContext) {

  val logger = LoggerFactory.getLogger(classOf[SingleThreadedDataExporter])

  val configSection = "intake24.asyncDataExporter"

  val bucketName = configuration.get[String](s"$configSection.s3.bucketName")
  val pathPrefix = configuration.get[String](s"$configSection.s3.pathPrefix")
  val urlExpirationTimeMinutes = configuration.get[Long](s"$configSection.s3.urlExpirationTimeMinutes")



/*
  def upload(task: ExportTaskHandle, deleteFile: Boolean): Future[URL] = task.result.flatMap {
    file =>
      logger.debug(s"[${task.id}] uploading ${file.getAbsolutePath} to S3")

      dataExportService.getTaskInfo(task.id) match {
        case Right(taskInfo) =>
        case Left(dbError) => Future.failed(dbError.exception)
      }

        yield {

          val dateFromString = DateTimeFormatter.ISO_DATE.format(taskInfo.dateFrom).replaceAll("Z", "")

          val dateToString = DateTimeFormatter.ISO_DATE.format(taskInfo.dateTo).replaceAll("Z", "")

          val fileName = s"$pathPrefix/intake24-${taskInfo.surveyId}-data-${task.id}-$dateFromString-$dateToString.csv"
        }
//AmazonServiceException, SdkClientException


      s3Client.putObject(config.s3BucketName, fileName, handles.file)

      s3Client.generatePresignedUrl(config.s3BucketName, fileName, Date.from(urlExpirationDate.toInstant()), HttpMethod.GET).toString
  }
*/
}
