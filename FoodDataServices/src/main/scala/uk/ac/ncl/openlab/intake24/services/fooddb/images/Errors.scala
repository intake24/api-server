package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError

sealed trait ImageServiceError

case class IOError(e: Throwable) extends ImageServiceError

case class FileTypeNotAllowed(e: Throwable) extends ImageServiceError

case class ImageStorageError(e: Throwable) extends ImageServiceError

case class ImageDatabaseError(e: LookupError) extends ImageServiceError

case class ImageProcessorError(e: Throwable) extends ImageServiceError

case class ImageDescriptor(id: Long, path: String)

case class ImageWithUrl(id: Long, url: String)
