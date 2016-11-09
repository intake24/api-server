package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.nio.file.Path

import org.apache.commons.io.FilenameUtils
import java.util.UUID

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.{AnyError, UnexpectedDatabaseError}


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

object ImageServiceOrDatabaseErrors {

  implicit def wrapImageServiceError[T](result: Either[ImageServiceError, T]): Either[ImageServiceOrDatabaseError, T] = result.left.map(ImageServiceErrorWrapper(_))

  implicit def wrapDatabaseError[T](result: Either[AnyError, T]): Either[ImageServiceOrDatabaseError, T] = result.left.map(DatabaseErrorWrapper(_))
}


trait ImageAdminService {
  def uploadSourceImage(suggestedPath: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceOrDatabaseError, Long]

  def deleteProcessedImages(ids: Seq[Long]): Either[ImageServiceOrDatabaseError, Unit]

  def processForAsServed(setId: String, sourceImageIds: Seq[Long]): Either[ImageServiceOrDatabaseError, Seq[AsServedImageDescriptor]]
  def processForSelectionScreen(pathPrefix: String, sourceImageId: Long): Either[ImageServiceOrDatabaseError, ImageDescriptor]

  def processForGuideImageBase(sourceImageId: Long): Either[ImageServiceOrDatabaseError, ImageDescriptor]
  def processForGuideImageOverlays(sourceImageId: Long): Either[ImageServiceOrDatabaseError, Seq[ImageDescriptor]]
}

object ImageAdminService {

  val asServedPathPrefix = "as_served"

  def randomName(originalName: String) = {
    val extension = "." + FilenameUtils.getExtension(originalName).toLowerCase()
    val randomName = UUID.randomUUID().toString() + extension
  }

  def getSourcePathForAsServed(setId: String, originalName: String): String =
    s"$asServedPathPrefix/$setId/${randomName(originalName)}"

  def ssiPrefixAsServed(setId: String): String =
    s"$asServedPathPrefix/$setId/selection"
}