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
import java.io.FileOutputStream

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

  def uploadImage(suggestedPath: String, sourceFile: Path): Either[ImageStorageError, String] = {
    val localPath = settings.baseDirectory + File.separator + suggestedPath

    val altPath = FilenameUtils.getFullPath(suggestedPath) + FilenameUtils.getBaseName(suggestedPath) +
      "-" + UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(suggestedPath)
    
    val localAltPath = settings.baseDirectory + File.separator + altPath

    try {
      val (destFile, relativePath) = {

        logger.debug(s"Attempting to create localPath")

        val f = new File(localPath)
        Option(f.getParentFile).foreach(_.mkdirs())

        if (f.createNewFile())
          (f, suggestedPath)
        else {
          logger.debug(s"$localPath already exists, attempting to create $localAltPath instead")
          val alt = new File(localAltPath)
          Option(alt.getParentFile).foreach(_.mkdirs())
          if (alt.createNewFile())
            (alt, altPath)
          else
            throw new RuntimeException(s"Failed to create file: $localAltPath")
        }
      }
      
      logger.debug(s"Copying ${sourceFile.toString()} to ${destFile.getAbsolutePath}")

      FileUtils.copyFile(sourceFile.toFile(), destFile)

      Right(relativePath)
    } catch {
      case e: Throwable => Left(ImageStorageError(e))
    }
  }
  
  def getUrl(path: String): String = s"${settings.urlPrefix}/$path"

  def downloadImage(path: String, dest: Path): Either[ImageStorageError, Unit] = {
    try {
      Files.copy(Paths.get(path), new FileOutputStream(dest.toFile()))
      Right(())
    } catch {
      case e: Throwable => Left(ImageStorageError(e))
    }
  }
}
