package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.nio.file.Path
import java.util.UUID

import org.apache.commons.io.FilenameUtils
import uk.ac.ncl.openlab.intake24.errors.AnyError

case class ImageDescriptor(id: Long, path: String)

case class ImageWithUrl(id: Long, url: String)

case class AsServedImageDescriptor(mainImage: ImageDescriptor, thumbnail: ImageDescriptor)

sealed trait ImageServiceOrDatabaseError {
  def exception: Throwable = this match {
    case ImageServiceErrorWrapper(error) => error.e
    case DatabaseErrorWrapper(error) => error.exception
  }
}

case class DatabaseErrorWrapper(error: AnyError) extends ImageServiceOrDatabaseError

case class ImageServiceErrorWrapper(error: ImageServiceError) extends ImageServiceOrDatabaseError

trait ImageAdminService {
  def uploadSourceImage(suggestedPath: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceOrDatabaseError, SourceImageRecord]

  def deleteSourceImages(ids: Seq[Long]): Either[ImageServiceOrDatabaseError, Unit]

  def deleteProcessedImages(ids: Seq[Long]): Either[ImageServiceOrDatabaseError, Unit]

  def processForAsServed(setId: String, sourceImageIds: Seq[Long]): Either[ImageServiceOrDatabaseError, Seq[AsServedImageDescriptor]]

  def processForSelectionScreen(pathPrefix: String, sourceImageId: Long): Either[ImageServiceOrDatabaseError, ImageDescriptor]

  def processForImageMapBase(imageMapId: String, sourceImageId: Long): Either[ImageServiceOrDatabaseError, ImageDescriptor]

  def generateImageMapOverlays(imageMapId: String, sourceId: Long, imageMap: AWTImageMap): Either[ImageServiceOrDatabaseError, Map[Int, ImageDescriptor]]
}

object ImageAdminService {

  implicit class WrapImageServiceError[T](result: Either[ImageServiceError, T]) {
    def wrapped: Either[ImageServiceOrDatabaseError, T] = result.left.map(ImageServiceErrorWrapper(_))
  }

  implicit class WrapDatabaseError[T](result: Either[AnyError, T]) {
    def wrapped: Either[ImageServiceOrDatabaseError, T] = result.left.map(DatabaseErrorWrapper(_))
  }

  val asServedPathPrefix = "as_served"

  val imageMapPathPrefix = "image_maps"

  def randomName(originalName: String) = {
    val extension = "." + FilenameUtils.getExtension(originalName).toLowerCase()

    UUID.randomUUID().toString() + extension
  }

  def getSourcePathForAsServed(setId: String, originalName: String): String =
    s"$asServedPathPrefix/$setId/${randomName(originalName)}"

  def getSourcePathForImageMap(id: String, originalName: String): String =
    s"$imageMapPathPrefix/$id/${randomName(originalName)}"

  def ssiPrefixAsServed(setId: String): String =
    s"$asServedPathPrefix/$setId/selection"
}