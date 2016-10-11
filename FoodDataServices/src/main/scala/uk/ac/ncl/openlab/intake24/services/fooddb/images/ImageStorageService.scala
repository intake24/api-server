package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.nio.file.Path

trait ImageStorageService {
  /**
   * Uploads image to the storage service, keeping the source file in place.
   */
  def uploadImage(suggestedPath: String, file: Path): Either[ImageStorageError, String]
  
  /**
   * Downloads a copy of an image from the storage service into the specified file.
   */
  def downloadImage(path: String, dest: Path): Either[ImageStorageError, Unit]
  
  def getUrl(path: String): String
  
  def deleteImage(path: String): Either[ImageStorageError, Unit]
}
