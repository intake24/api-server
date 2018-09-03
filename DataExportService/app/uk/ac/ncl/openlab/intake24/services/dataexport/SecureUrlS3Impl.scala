package uk.ac.ncl.openlab.intake24.services.dataexport

import java.io.File
import java.net.URL
import java.time.ZonedDateTime
import java.util.Date

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import javax.inject.{Inject, Named, Singleton}
import org.slf4j.LoggerFactory
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class SecureUrlS3Impl @Inject()(configuration: Configuration,
                                s3Client: AmazonS3,
                                @Named("intake24") implicit val executionContext: ExecutionContext) extends SecureUrlService {

  val logger = LoggerFactory.getLogger(classOf[SecureUrlS3Impl])

  val configSection = "intake24.dataExport.secureUrl.S3"

  val bucketName = configuration.get[String](s"$configSection.bucketName")
  val pathPrefix = configuration.get[String](s"$configSection.pathPrefix")

  def createUrl(fileName: String, file: File, expirationDate: ZonedDateTime): Try[URL] =
    Try {
      val objName = s"$pathPrefix/$fileName"
      s3Client.putObject(bucketName, objName, file)
      s3Client.generatePresignedUrl(bucketName, objName, Date.from(expirationDate.toInstant), HttpMethod.GET)
    }
}
