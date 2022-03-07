package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.nio.file.{CopyOption, Files, Path, StandardCopyOption}
import javax.inject.{Inject, Singleton}

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3ObjectInputStream
import org.slf4j.LoggerFactory

case class CloudFrontS3ImageStorageSettings(bucketName: String, bucketPathPrefix: String, imageUrl: String, cloudFrontPathPrefix: String)

@Singleton
class ImageStorageCloudFrontS3 @Inject()(s3client: AmazonS3,
                               settings: CloudFrontS3ImageStorageSettings) extends ImageStorageService {

  val logger = LoggerFactory.getLogger(classOf[ImageStorageCloudFrontS3])

  val configSection = "intake24.images.S3Storage"

  def deleteImage(path: String): Either[ImageStorageError, Unit] =
    try {
      logger.debug(s"Attempting to delete $path")

      s3client.deleteObject(settings.bucketName, settings.bucketPathPrefix + "/" + path)
      Right(())
    } catch {
      case e: Throwable => Left(ImageStorageError(e))
    }

  def uploadImage(suggestedPath: String, sourceFile: Path): Either[ImageStorageError, String] = {

    val path = settings.bucketPathPrefix + "/" + suggestedPath


    try {

      s3client.putObject(settings.bucketName, path, sourceFile.toFile)

      logger.debug(s"Uploading ${sourceFile.toString()} to S3 bucket ${settings.bucketName} using name $path")
      logger.debug(s"URL: ${getUrl(path)}")
      Right(suggestedPath)
    } catch {
      case e: Throwable => Left(ImageStorageError(e))
    }
  }

  def getUrl(path: String): String = s"https://${settings.imageUrl}/${settings.cloudFrontPathPrefix}/${path}"

  def downloadImage(path: String, dest: Path): Either[ImageStorageError, Unit] = {

    var stream: S3ObjectInputStream = null

    try {

      stream = s3client.getObject(settings.bucketName, settings.bucketPathPrefix + "/" + path).getObjectContent()

      Files.copy(stream, dest, StandardCopyOption.REPLACE_EXISTING)

      Right(())
    } catch {
      case e: Throwable => Left(ImageStorageError(e))
    } finally {
      if (stream != null)
        stream.close()
    }
  }
}
