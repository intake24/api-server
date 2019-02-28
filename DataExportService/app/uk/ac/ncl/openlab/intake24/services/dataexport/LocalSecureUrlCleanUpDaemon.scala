package uk.ac.ncl.openlab.intake24.services.dataexport

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{DirectoryStream, Files, Path, Paths}
import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import javax.inject.Named
import org.slf4j.LoggerFactory
import play.api.Configuration

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class LocalSecureUrlCleanUpDaemon @Inject()(config: Configuration,
                                            system: ActorSystem,
                                            @Named("intake24") implicit val executionContext: ExecutionContext,
                                           ) {

  private val logger = LoggerFactory.getLogger(classOf[LocalSecureUrlCleanUpDaemon])

  private val validityPeriod = config.get[FiniteDuration]("intake24.dataExport.secureUrl.validityPeriod")
  private val cleanupInterval = config.get[FiniteDuration]("intake24.dataExport.secureUrl.local.cleanupInterval")
  private val dirPath = config.get[String]("intake24.dataExport.secureUrl.local.directory")

  val dir = Paths.get(dirPath)

  if (Files.exists(dir) && Files.isDirectory(dir)) {
    system.scheduler.schedule(0.minutes, cleanupInterval) {
      val minCreatedAt = Instant.now().minus(validityPeriod.toMillis, ChronoUnit.MILLIS)

      logger.debug("Deleting files created before " + minCreatedAt.toString)

      var stream: DirectoryStream[Path] = null

      try {
        stream = Files.newDirectoryStream(dir, (entry: Path) => Files.isRegularFile(entry))
        stream.iterator().asScala.foreach {
          file =>
            val attrs = Files.readAttributes(file, classOf[BasicFileAttributes])
            val createdAt = attrs.creationTime().toInstant

            if (createdAt.isBefore(minCreatedAt)) {
              logger.debug(s"Deleting ${file.toString}")
              Files.delete(file)
            }
        }
      } finally  {
        if (stream != null)
          stream.close()
      }
    }
  } else {
    logger.warn("intake24.dataExport.secureUrl.local.directory does not point to a directory, file cleanup service will not start")
  }
}
