package uk.ac.ncl.openlab.intake24.services.fooddb.images

import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.slf4j.LoggerFactory

import java.io.{File, FileOutputStream}
import java.nio.file.{CopyOption, Files, Path, Paths, StandardCopyOption}
import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class ImageStorageLocal @Inject()(val settings: LocalImageStorageSettings) extends ImageStorageService {

  val logger = LoggerFactory.getLogger(classOf[ImageStorageLocal])

  private def resolveLocalPath(path: String): Either[ImageStorageError, String] = {
    val localPath = settings.baseDirectory + File.separator + path

    if (FilenameUtils.directoryContains(settings.baseDirectory, FilenameUtils.normalize(localPath)))
      Right(localPath)
    else
      Left(ImageStorageError(new RuntimeException("Paths outside the base directory are not allowed")))
  }

  def deleteImage(path: String): Either[ImageStorageError, Unit] = {
    resolveLocalPath(path).flatMap(localPath => {
      try {
        logger.debug(s"Attempting to delete $localPath")
        Files.delete(Paths.get(localPath))
        Right(())
      } catch {
        case e: Throwable => Left(ImageStorageError(e))
      }
    })
  }

  private def uploadImpl(suggestedPath: String, suggestedAltPath: String,
                         localPath: String, localAltPath: String,
                         sourceFile: Path): Either[ImageStorageError, String] = {
    try {
      val (destFile, relativePath) = {

        logger.debug(s"Attempting to create $localPath")

        val f = new File(localPath)
        Option(f.getParentFile).foreach(_.mkdirs())

        if (f.createNewFile())
          (f, suggestedPath)
        else {
          logger.debug(s"$localPath already exists, attempting to create $localAltPath instead")
          val alt = new File(localAltPath)
          Option(alt.getParentFile).foreach(_.mkdirs())
          if (alt.createNewFile())
            (alt, suggestedAltPath)
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

  private def generateAltName(path: String): String =
    if (path == null || path.isEmpty)
      UUID.randomUUID().toString()
    else {
      val extension = FilenameUtils.getExtension(path)
      val newExtension = if (extension.isEmpty) "" else "." + extension
      FilenameUtils.getFullPath(path) + FilenameUtils.getBaseName(path) + "-" + UUID.randomUUID().toString() + newExtension
    }

  def uploadImage(suggestedPath: String, sourceFile: Path): Either[ImageStorageError, String] = {
    val nonEmptySuggestedPath = if (suggestedPath == null || suggestedPath.isEmpty) UUID.randomUUID().toString() else suggestedPath
    val suggestedAltPath = generateAltName(suggestedPath)

    for (localPath <- resolveLocalPath(nonEmptySuggestedPath);
         localAltPath <- resolveLocalPath(suggestedAltPath);
         relativePath <- uploadImpl(nonEmptySuggestedPath, suggestedAltPath, localPath, localAltPath, sourceFile))
      yield relativePath
  }

  def getUrl(path: String): String = s"${settings.urlPrefix}/$path"

  def downloadImage(path: String, dest: Path): Either[ImageStorageError, Unit] = {
    resolveLocalPath(path).flatMap(localPath => {
      try {
        Files.copy(Paths.get(localPath), dest, StandardCopyOption.REPLACE_EXISTING)
        Right(())
      } catch {
        case e: Throwable => Left(ImageStorageError(e))
      }
    })
  }
}
