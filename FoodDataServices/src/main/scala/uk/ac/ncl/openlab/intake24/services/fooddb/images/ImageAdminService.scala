package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File
import java.nio.file.Path
import org.apache.commons.io.FilenameUtils
import java.util.UUID

case class AsServedSetDescriptors(selectionImage: ImageDescriptor, images: Seq[AsServedImageDescriptor])

case class AsServedImageDescriptor(mainImage: ImageDescriptor, thumbnail: ImageDescriptor)

trait ImageAdminService {
  def uploadSourceImage(suggestedPath: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceError, Long]

  def processForAsServed(setId: String, sourceImageIds: Seq[Long]): Either[ImageServiceError, AsServedSetDescriptors]
  def processForGuideImageBase(sourceImageId: Long): Either[ImageServiceError, ImageDescriptor]
  def processForGuideImageOverlays(sourceImageId: Long): Either[ImageServiceError, Seq[ImageDescriptor]]
}

object ImageAdminService {

  val asServedPathPrefix = "as_served"

  def getSourcePathForAsServed(setId: String, originalName: String): String = {
    val extension = "." + FilenameUtils.getExtension(originalName).toLowerCase()
    val randomName = UUID.randomUUID().toString() + extension
    
    s"$asServedPathPrefix/$setId/$randomName"
  }
}