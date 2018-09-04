package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.nio.file.Path

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory

@Singleton
class ImageStorageS3ReadOnly @Inject()(settings: S3ImageStorageSettings) extends ImageStorageService {

  val logger = LoggerFactory.getLogger(classOf[ImageStorageS3ReadOnly])

  val configSection = "intake24.images.S3Storage"

  private def notSupported = Left(ImageStorageError(new NotImplementedError("Not supported by read-only storage implementation")))

  def deleteImage(path: String): Either[ImageStorageError, Unit] = notSupported

  def uploadImage(suggestedPath: String, sourceFile: Path): Either[ImageStorageError, String] = notSupported

  def getUrl(path: String): String = s"https://${settings.bucketName}.s3.amazonaws.com/${settings.pathPrefix}/${path}"

  def downloadImage(path: String, dest: Path): Either[ImageStorageError, Unit] = notSupported
}
