package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File


sealed trait ImageProcessorError extends ImageServiceError


sealed trait ImageStorageError extends ImageServiceError

case class IOError(e: Throwable) extends ImageStorageError with ImageProcessorError


sealed trait ImageServiceError

case class DatabaseError(e: Throwable) extends ImageServiceError




sealed trait ImagePurpose

case object AsServedImage

case object AsServedThumbnail

case object GuideImageBase

case object GuideImageOverlay


case class ImageDescriptor(id: Long, path: String)

case class AsServedImageDescriptor(mainImage: ImageDescriptor, thumbnail: ImageDescriptor)


