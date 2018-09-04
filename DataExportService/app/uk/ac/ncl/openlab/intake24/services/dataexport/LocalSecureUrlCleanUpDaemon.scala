package uk.ac.ncl.openlab.intake24.services.dataexport

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path, Paths}
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
class SecureUrlFileCleanUpDaemon @Inject()(config: Configuration,
                                           system: ActorSystem,
                                           @Named("intake24") implicit val executionContext: ExecutionContext,
                                 ) {

  private val logger = LoggerFactory.getLogger(classOf[SecureUrlFileCleanUpDaemon])

  private val cleanupInterval = config.get[Int]("intake24.localDownloads.cleanupIntervalSeconds")
  private val lifeTime = config.get[Int]("intake24.localDownloads.fileLifeTimeSeconds")
  private val dirPath = config.get[String]("intake24.localDownloads.directory")

  val dir = Paths.get(dirPath)

  if (Files.exists(dir) && Files.isDirectory(dir)) {
    system.scheduler.schedule(0.minutes, cleanupInterval.seconds) {

      val maxCreatedAt = Instant.now().minus(lifeTime, ChronoUnit.SECONDS)

      Files.newDirectoryStream(dir, (entry: Path) => Files.isRegularFile(entry)).iterator().asScala.foreach {
        file =>
          val attrs = Files.readAttributes(file, classOf[BasicFileAttributes])
          val createdAt = attrs.creationTime().toInstant

          if (createdAt.isBefore(maxCreatedAt)) {
            logger.debug(s"Deleting ${file.toString}")
            Files.delete(file)
          }
      }
    }
  } else {
    logger.warn("intake24.localDownloads.directory does not point to a directory, file cleanup service will not start")
  }
}
