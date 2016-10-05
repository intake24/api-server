package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File
import java.util.UUID

import scala.Left
import scala.Right

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils

import javax.inject.Inject

class ImageStorageLocal @Inject() (val settings: LocalImageStorageSettings) extends ImageStorageService {
  def deleteImage(name: String): Either[ImageStorageError, Unit] = {
    try {
      val path = settings.baseDirectory + File.separator + name
      val deleted = new File(path).delete()

      if (!deleted)
        throw new RuntimeException("Failed to delete file: $path")

      Right(())
    } catch {
      case e: Throwable => Left(IOError(e))
    }
  }

  def uploadImage(suggestedName: String, sourceFile: File): Either[ImageStorageError, String] = {
    val path = s"${settings.baseDirectory}${File.separator}${suggestedName}"

    val altPath = s"${settings.baseDirectory}${File.separator}${FilenameUtils.getBaseName(suggestedName)}-${UUID.randomUUID().toString()}-${FilenameUtils.getExtension(suggestedName)}"

    try {
      val destFile = {
        val f = new File(path)
        if (f.createNewFile())
          f
        else {
          val alt = new File(altPath)
          if (alt.createNewFile())
            alt
          else
            throw new RuntimeException("Failed to create file: $altPath")
        }
      }

      FileUtils.copyFile(sourceFile, destFile)

      Right(destFile.getPath)
    } catch {
      case e: Throwable => Left(IOError(e))
    }
  }

  def downloadImage(path: String): Either[ImageStorageError, File] = {
    val file = new File(path)
    
    if (file.exists)
      Right(file)
    else
      Left(new IOError(new RuntimeException(s"File does not exists: $path")))
  }
}