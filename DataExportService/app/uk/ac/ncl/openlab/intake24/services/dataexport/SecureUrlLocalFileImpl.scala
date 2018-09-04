package uk.ac.ncl.openlab.intake24.services.dataexport

import java.io.File
import java.net.URL
import java.nio.file.{Files, Paths}
import java.time.ZonedDateTime
import java.util.UUID

import javax.inject.{Inject, Named, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class SecureUrlLocalFileImpl @Inject()(configuration: Configuration,
                                       @Named("intake24") implicit val executionContext: ExecutionContext) extends SecureUrlService {

  val logger = LoggerFactory.getLogger(classOf[SecureUrlLocalFileImpl])

  val dirPath = Paths.get(configuration.get[String]("intake24.dataExport.secureUrl.local.directory"))

  val apiServerUrlPrefix = configuration.get[String]("intake24.apiServerUrl")

  def createUrl(fileName: String, file: File, expirationDate: ZonedDateTime): Try[URL] =
    Try {
      val secureName = UUID.randomUUID().toString()

      val srcPath = Paths.get(file.getAbsolutePath())
      val destPath = dirPath.resolve(s"$secureName.$fileName")

      Files.move(srcPath, destPath)



      new URL(s"$apiServerUrlPrefix/data-export/download?key=$secureName")
    }
}
