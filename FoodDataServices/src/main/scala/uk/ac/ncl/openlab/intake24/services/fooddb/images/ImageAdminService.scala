package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File
import java.nio.file.Path
import org.apache.commons.io.FilenameUtils
import java.util.UUID

case class AsServedImageDescriptor(mainImage: ImageDescriptor, thumbnail: ImageDescriptor)

trait ImageAdminService {
  def uploadSourceImage(suggestedPath: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceError, Long]

  def processForAsServed(setId: String, sourceImageIds: Seq[Long]): Either[ImageServiceError, Seq[AsServedImageDescriptor]]
  def processForSelectionScreen(pathPrefix: String, sourceImageId: Long): Either[ImageServiceError, ImageDescriptor]

  def processForGuideImageBase(sourceImageId: Long): Either[ImageServiceError, ImageDescriptor]
  def processForGuideImageOverlays(sourceImageId: Long): Either[ImageServiceError, Seq[ImageDescriptor]]
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
    s"$asServedPathPrefix/$setId/selection}"
}