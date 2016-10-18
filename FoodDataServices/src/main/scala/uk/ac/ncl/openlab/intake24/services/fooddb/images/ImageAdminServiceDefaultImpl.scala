package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.util.UUID
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Path
import javax.inject.Inject
import java.io.File
import java.nio.file.Files
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import scala.sys.process

class ImageAdminServiceDefaultImpl @Inject() (val imageDatabase: ImageDatabaseService, val imageProcessor: ImageProcessor, val storage: ImageStorageService,
  val fileTypeAnalyzer: FileTypeAnalyzer)
    extends ImageAdminService {

  private val allowedFileTypes = Seq("image/jpeg", "image/png", "image/svg+xml")

  private val sourcePathPrefix = "source"
  
  private val asServedPathPrefix = "as_served"
  
  private val logger = LoggerFactory.getLogger(classOf[ImageAdminServiceDefaultImpl])

  private def withTempDir[T](block: Path => Either[ImageServiceError, T]): Either[ImageServiceError, T] =
    try {
      val tempDir = Files.createTempDirectory("intake24")
      try {
        block(tempDir)
      } finally {
        Files.walkFileTree(tempDir, new SimpleFileVisitor[Path]() {
          override def visitFile(file: Path, attrs: BasicFileAttributes) = {
            Files.delete(file)
            FileVisitResult.CONTINUE
          }

          override def postVisitDirectory(dir: Path, e: IOException) = {
            Files.delete(dir)
            FileVisitResult.CONTINUE
          }
        })
      }
    } catch {
      case e: IOException => Left(IOError(e))
    }

  def wrapDatabaseError[T](e: Either[LookupError, T]): Either[ImageDatabaseError, T] = e match {
    case Right(result) => Right(result)
    case Left(e) => Left(ImageDatabaseError(e))
  }

  def checkFileType(path: Path): Either[ImageServiceError, Unit] = {
    val actualType = fileTypeAnalyzer.getFileMimeType(path)

    if (allowedFileTypes.exists(t => actualType.startsWith(t)))
      Right(())
    else
      Left(FileTypeNotAllowed(new RuntimeException(s"""File type not allowed: $actualType, allowed types: ${allowedFileTypes.mkString(", ")}""")))
  }

  def uploadSourceImageForAsServed(setId: String, originalName: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceError, Long] = {
    val extension = "." + FilenameUtils.getExtension(originalName).toLowerCase()
    val randomName = UUID.randomUUID().toString() + extension 
    val path = s"$asServedPathPrefix/$setId/$randomName"
    uploadSourceImage(path, source, keywords, uploaderName)
  }
  
  // TODO: if the database operation fails, images need to be deleted from storage.
  // Failing to do that won't break anything, but will result in unused files.
  // Maybe some garbage collection is a better idea?
  def uploadSourceImage(suggestedPath: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceError, Long] =
    for (
      _ <- {
        logger.debug("Checking file type")
        checkFileType(source).right
      };
      actualPath <- {
        logger.debug("Uploading source image to storage")
        storage.uploadImage(sourcePathPrefix + File.separator + suggestedPath, source).right
      };
      id <- {
        logger.debug("Creating a database record for the source image")
        wrapDatabaseError(imageDatabase.createSourceImageRecords(Seq(SourceImageRecord(actualPath, keywords, uploaderName)))).right
      }
    ) yield id.head

  private case class UploadedAsServedPaths(mainImage: String, thumbnail: String)

  private def processAndUploadAsServed(setId: String, sources: Seq[ImageDescriptor]): Either[ImageServiceError, Seq[UploadedAsServedPaths]] = {
    withTempDir {
      tempDir =>
        def rec(src: Seq[ImageDescriptor], acc: Vector[UploadedAsServedPaths]): Either[ImageServiceError, Vector[UploadedAsServedPaths]] =
          if (src.isEmpty)
            Right(acc)
          else {
            val desc = src.head

            val extension = "." + FilenameUtils.getExtension(desc.path).toLowerCase()

            val srcPath = Files.createTempFile(tempDir, "intake24", extension)
            val dstMainPath = Files.createTempFile(tempDir, "intake24", extension)
            val dstThumbnailPath = Files.createTempFile(tempDir, "intake24", extension)

            val randomName = UUID.randomUUID().toString() + extension

            val processResult = for (
              _ <- storage.downloadImage(desc.path, srcPath).right;
              _ <- imageProcessor.processForAsServed(srcPath, dstMainPath, dstThumbnailPath).right;
              actualMainPath <- storage.uploadImage(s"as_served/$setId/$randomName", dstMainPath).right;
              actualThumbPath <- storage.uploadImage(s"as_served/$setId/thumbnails/$randomName", dstThumbnailPath).right
            ) yield UploadedAsServedPaths(actualMainPath, actualThumbPath)

            processResult match {
              case Right(paths) => rec(src.tail, acc :+ paths)
              case Left(error) => Left(error)
            }
          }

        rec(sources, Vector())
    }
  }

  private def mkProcessedMainImageRecords(sourceIds: Seq[Long], paths: Seq[UploadedAsServedPaths]): Seq[ProcessedImageRecord] =
    sourceIds.zip(paths).map { case (id, paths) => ProcessedImageRecord(paths.mainImage, id, ProcessedImagePurpose.AsServedMainImage) }

  private def mkProcessedThumbnailRecords(sourceIds: Seq[Long], paths: Seq[UploadedAsServedPaths]): Seq[ProcessedImageRecord] =
    sourceIds.zip(paths).map { case (id, paths) => ProcessedImageRecord(paths.thumbnail, id, ProcessedImagePurpose.AsServedThumbnail) }

  def processForAsServed(setId: String, sourceImageIds: Seq[Long]): Either[ImageServiceError, Seq[AsServedImageDescriptor]] =
    for (
      sources <- {
        logger.debug("Getting source image descriptors");
        wrapDatabaseError(imageDatabase.getSourceImageDescriptors(sourceImageIds)).right
      };
      uploadedPaths <- {
        logger.debug("Processing and uploading as served images")
        processAndUploadAsServed(setId, sources).right
      };
      mainImageIds <- {
        logger.debug("Creating processed image records for main images")
        wrapDatabaseError(imageDatabase.createProcessedImageRecords(mkProcessedMainImageRecords(sourceImageIds, uploadedPaths))).right
      };
      thumbnailIds <- {
        logger.debug("Creating processed image records for thumbnail images")
        wrapDatabaseError(imageDatabase.createProcessedImageRecords(mkProcessedThumbnailRecords(sourceImageIds, uploadedPaths))).right
      }
    ) yield {
      uploadedPaths.zip(mainImageIds).zip(thumbnailIds).map {
        case (((UploadedAsServedPaths(mainImagePath, thumbnailPath), mainImageId), thumbnailId)) =>
          AsServedImageDescriptor(ImageDescriptor(mainImageId, mainImagePath), ImageDescriptor(thumbnailId, thumbnailPath))
      }
    }

  def processForGuideImageBase(sourceImageId: Long): Either[ImageServiceError, ImageDescriptor] = {
    ???
  }

  def processForGuideImageOverlays(sourceImageId: Long): Either[ImageServiceError, Seq[ImageDescriptor]] = {
    ???
  }

  def uploadSourceImage(file: File, keywords: Seq[String]): Either[ImageServiceError, Long] = {
    ???
  }
}