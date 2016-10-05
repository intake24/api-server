package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File

trait ImageStorageService {
  /**
   * Uploads image to the storage service, keeping the source file in place.
   */
  def uploadImage(suggestedPath: String, file: File): Either[ImageStorageError, String]
  
  /**
   * Downloads a copy of an image from the storage service into a temporary file. Caller is responsible for deleting the file when they're done with it.
   */
  def downloadImage(path: String): Either[ImageStorageError, File]
  
  def deleteImage(path: String): Either[ImageStorageError, Unit]
}
