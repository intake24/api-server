package uk.ac.ncl.openlab.intake24.services.dataexport

import java.net.URL
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

import javax.inject.{Inject, Named, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.errors.{AnyError, ErrorUtils}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.DataExportService

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataExportSecureUrlUploader @Inject()(configuration: Configuration,
                                            dataExportService: DataExportService,
                                            secureUrlService: SecureUrlService,
                                            @Named("intake24") implicit val executionContext: ExecutionContext) {

  val logger = LoggerFactory.getLogger(classOf[DataExportSecureUrlUploader])

  val validityPeriod = configuration.get[FiniteDuration]("intake24.dataExport.secureUrl.validityPeriod")

  def upload(task: ExportTaskHandle, fileName: String): Future[Either[AnyError, URL]] =
    task.result.map {
      fileResult =>
        (for (file <- fileResult;
              urlExpirationDate <- Right(ZonedDateTime.now().plus(validityPeriod.toMillis, ChronoUnit.MILLIS));
              url <- ErrorUtils.fromTry(secureUrlService.createUrl(fileName, file, urlExpirationDate));
              _ <- dataExportService.setExportTaskDownloadUrl(task.id, url, urlExpirationDate))
          yield url).left.map {
          error =>
            logger.error(s"[${task.id}] Failed to create secure URL", error.exception)
            dataExportService.setExportTaskDownloadFailed(task.id, error.exception).left.map {
              setError =>
                logger.warn(s"[${task.id}] could not set failed status for data export download", setError.exception)
            }
            error
        }
    }
}
