package uk.ac.ncl.openlab.intake24.services.fooddb.images

sealed trait ImageServiceError {
  val e: Throwable
}

case class IOError(e: Throwable) extends ImageServiceError

case class FileTypeNotAllowed(e: Throwable) extends ImageServiceError

case class ImageStorageError(e: Throwable) extends ImageServiceError

case class ImageProcessorError(e: Throwable) extends ImageServiceError
