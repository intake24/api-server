package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File
import java.nio.file.Path

case class AsServedImageDescriptor(mainImage: ImageDescriptor, thumbnail: ImageDescriptor)

trait ImageAdminService {
  def uploadSourceImage(suggestedPath: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceError, Long]
  def uploadSourceImageForAsServed(setId: String, originalFileName: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceError, Long]
  def processForAsServed(setId: String, sourceImageIds: Seq[Long]): Either[ImageServiceError, Seq[AsServedImageDescriptor]]
  def processForGuideImageBase(sourceImageId: Long): Either[ImageServiceError, ImageDescriptor]
  def processForGuideImageOverlays(sourceImageId: Long): Either[ImageServiceError, Seq[ImageDescriptor]]
}