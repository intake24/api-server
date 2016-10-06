package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File
import java.util.UUID

import scala.Left
import scala.Right

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils

import javax.inject.Inject
import javax.inject.Singleton
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.CopyOption
import java.nio.file.Paths
import org.slf4j.LoggerFactory

@Singleton
class ImageStorageLocal @Inject() (val settings: LocalImageStorageSettings) extends ImageStorageService {
  
  val logger = LoggerFactory.getLogger(classOf[ImageStorageLocal])
  
  def deleteImage(path: String): Either[ImageStorageError, Unit] =
    try {
      logger.debug("Attempting to delete $path")
      Files.delete(Paths.get(settings.baseDirectory + File.separator + path))
      Right(())
    } catch {
      case e: Throwable => Left(ImageStorageError(e))
    }

  def uploadImage(suggestedName: String, sourceFile: Path): Either[ImageStorageError, String] = {
    val path = s"${settings.baseDirectory}${File.separator}${suggestedName}"

    val altPath = s"${settings.baseDirectory}${File.separator}${FilenameUtils.getBaseName(suggestedName)}-${UUID.randomUUID().toString()}-${FilenameUtils.getExtension(suggestedName)}"

    try {
      val destFile = {
        
        logger.debug(s"Attempting to create $path")
        
        val f = new File(path)
        if (f.createNewFile())
          f
        else {
          logger.debug(s"$path already exists, attempting to create $altPath instead")
          val alt = new File(altPath)
          if (alt.createNewFile())
            alt
          else
            throw new RuntimeException(s"Failed to create file: $altPath")
        }
      }

      FileUtils.copyFile(sourceFile.toFile(), destFile)

      Right(destFile.getPath)
    } catch {
      case e: Throwable => Left(ImageStorageError(e))
    }
  }

  def downloadImage(path: String, dest: Path): Either[ImageStorageError, Unit] = {
    try {
      Files.copy(Paths.get(path), dest)
      Right(())
    } catch {
      case e: Throwable => Left(ImageStorageError(e))
    }

  }
}