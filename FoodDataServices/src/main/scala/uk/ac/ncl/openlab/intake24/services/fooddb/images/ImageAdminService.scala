package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File

trait ImageAdminService {
  def uploadSourceImage(file: File, keywords: Seq[String]): Either[ImageServiceError, Long]
  def processForAsServed(sourceImageIds: Seq[Long]): Either[ImageServiceError, Seq[AsServedImageDescriptor]]
  def processForGuideImageBase(sourceImageId: Long): Either[ImageServiceError, ImageDescriptor]
  def processForGuideImageOverlays(sourceImageId: Long): Either[ImageServiceError, Map[Int, ImageDescriptor]]
}