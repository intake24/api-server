package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.{File, IOException}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}
import java.util.UUID
import javax.inject.Inject

import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageServiceOrDatabaseErrors._

class ImageAdminServiceDefaultImpl @Inject()(val imageDatabase: ImageDatabaseService, val imageProcessor: ImageProcessor, val storage: ImageStorageService,
                                             val fileTypeAnalyzer: FileTypeAnalyzer)
  extends ImageAdminService {

  private val allowedFileTypes = Seq("image/jpeg", "image/png", "image/svg+xml")

  private val sourcePathPrefix = "source"

  private val logger = LoggerFactory.getLogger(classOf[ImageAdminServiceDefaultImpl])

  private def withTempDir[T](block: Path => Either[ImageServiceOrDatabaseError, T]): Either[ImageServiceOrDatabaseError, T] =
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
      case e: IOException => Left(ImageServiceErrorWrapper(IOError(e)))
    }


  def checkFileType(path: Path): Either[ImageServiceError, Unit] = {
    val actualType = fileTypeAnalyzer.getFileMimeType(path)

    if (allowedFileTypes.exists(t => actualType.startsWith(t)))
      Right(())
    else
      Left(FileTypeNotAllowed(new RuntimeException(s"""File type not allowed: $actualType, allowed types: ${allowedFileTypes.mkString(", ")}""")))
  }

  private def deleteImagesFromStorageImpl(paths: Seq[String]): Either[ImageServiceError, Unit] = {
    // TODO: change to parallel
    val results = paths.map {
      path => storage.deleteImage(path)
    }

    results.find(_.isLeft) match {
      case Some(Left(error)) => Left(error)
      case _ => Right(())
    }
  }

  def deleteProcessedImages(ids: Seq[Long]): Either[ImageServiceOrDatabaseError, Unit] = {
    for (
      paths <- wrapDatabaseError(imageDatabase.getProcessedImageRecords(ids)).right.map {
        _.map(_.path)
      }.right;
      _ <- wrapImageServiceError(deleteImagesFromStorageImpl(paths)).right;
      _ <- wrapDatabaseError(imageDatabase.deleteProcessedImageRecords(ids)).right
    ) yield ()
  }

  // TODO: if the database operation fails, images need to be deleted from storage.
  // Failing to do that won't break anything, but will result in unused files.
  // Maybe some garbage collection is a better idea?
  def uploadSourceImage(suggestedPath: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceOrDatabaseError, Long] =
  withTempDir {
    tempDir =>
      val extension = getExtension(source.toString)
      val thumbDst = Files.createTempFile(tempDir, "intake24", extension)

      for (
        _ <- {
          logger.debug("Checking file type")
          wrapImageServiceError(checkFileType(source)).right
        };
        _ <- {
          logger.debug("Generating fixed size thumbnail")
          wrapImageServiceError(imageProcessor.processForSourceThumbnail(source, thumbDst)).right
        };
        actualPath <- {
          logger.debug("Uploading source image to storage")
          wrapImageServiceError(storage.uploadImage(sourcePathPrefix + File.separator + suggestedPath, source)).right
        };
        actualThumbPath <- {
          logger.debug("Uploading thumbnail image to storage")
          wrapImageServiceError(storage.uploadImage(sourcePathPrefix + "/thumbnails/" + suggestedPath, thumbDst)).right
        };
        id <- {
          logger.debug("Creating a database record for the source image")
          wrapDatabaseError(imageDatabase.createSourceImageRecords(Seq(NewSourceImageRecord(actualPath, actualThumbPath, keywords, uploaderName)))).right
        }
      ) yield id.head
  }

  private case class AsServedImagePaths(mainImage: String, thumbnail: String)

  private def getExtension(path: String) = "." + FilenameUtils.getExtension(path).toLowerCase()

  private def processAndUploadAsServed(setId: String, sources: Seq[ImageDescriptor]): Either[ImageServiceOrDatabaseError, Seq[AsServedImagePaths]] = {
    withTempDir {
      tempDir =>
        def rec(src: Seq[ImageDescriptor], acc: Vector[AsServedImagePaths]): Either[ImageServiceOrDatabaseError, Vector[AsServedImagePaths]] =
          if (src.isEmpty)
            Right(acc)
          else {
            val desc = src.head

            val extension = getExtension(desc.path)

            val srcPath = Files.createTempFile(tempDir, "intake24", extension)
            val dstMainPath = Files.createTempFile(tempDir, "intake24", extension)
            val dstThumbnailPath = Files.createTempFile(tempDir, "intake24", extension)

            val randomName = UUID.randomUUID().toString() + extension

            val processResult = for (
              _ <- wrapImageServiceError(storage.downloadImage(desc.path, srcPath)).right;
              _ <- wrapImageServiceError(imageProcessor.processForAsServed(srcPath, dstMainPath, dstThumbnailPath)).right;
              actualMainPath <- wrapImageServiceError(storage.uploadImage(s"as_served/$setId/$randomName", dstMainPath)).right;
              actualThumbPath <- wrapImageServiceError(storage.uploadImage(s"as_served/$setId/thumbnails/$randomName", dstThumbnailPath)).right
            ) yield AsServedImagePaths(actualMainPath, actualThumbPath)

            processResult match {
              case Right(paths) => rec(src.tail, acc :+ paths)
              case Left(error) => Left(error)
            }
          }

        rec(sources, Vector())
    }
  }

  private def processAndUploadSelectionScreenImage(pathPrefix: String, source: ImageDescriptor): Either[ImageServiceOrDatabaseError, String] =
    withTempDir {
      tempDir =>
        val extension = getExtension(source.path)

        val srcPath = Files.createTempFile(tempDir, "intake24", extension)
        val dstPath = Files.createTempFile(tempDir, "intake24", extension)

        val randomName = UUID.randomUUID().toString() + extension

        for (
          _ <- wrapImageServiceError(storage.downloadImage(source.path, srcPath)).right;
          _ <- wrapImageServiceError(imageProcessor.processForSelectionScreen(srcPath, dstPath)).right;
          actualPath <- wrapImageServiceError(storage.uploadImage(s"$pathPrefix/$randomName", dstPath)).right
        ) yield actualPath
    }

  private def mkProcessedMainImageRecords(sourceIds: Seq[Long], paths: Seq[AsServedImagePaths]): Seq[ProcessedImageRecord] =
    sourceIds.zip(paths).map { case (id, paths) => ProcessedImageRecord(paths.mainImage, id, ProcessedImagePurpose.AsServedMainImage) }

  private def mkProcessedThumbnailRecords(sourceIds: Seq[Long], paths: Seq[AsServedImagePaths]): Seq[ProcessedImageRecord] =
    sourceIds.zip(paths).map { case (id, paths) => ProcessedImageRecord(paths.thumbnail, id, ProcessedImagePurpose.AsServedThumbnail) }

  def processForAsServed(setId: String, sourceImageIds: Seq[Long]): Either[ImageServiceOrDatabaseError, Seq[AsServedImageDescriptor]] =
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
        val records = mkProcessedMainImageRecords(sourceImageIds, uploadedPaths)
        logger.debug(s"Creating processed image records for main images")
        records.foreach(r => logger.debug(r.toString()))
        wrapDatabaseError(imageDatabase.createProcessedImageRecords(records)).right
      };
      thumbnailIds <- {
        val records = mkProcessedThumbnailRecords(sourceImageIds, uploadedPaths)
        logger.debug("Creating processed image records for thumbnail images")
        records.foreach(r => logger.debug(r.toString()))
        wrapDatabaseError(imageDatabase.createProcessedImageRecords(records)).right
      }
    ) yield {
      uploadedPaths.zip(mainImageIds).zip(thumbnailIds).map {
        case (((AsServedImagePaths(mainImagePath, thumbnailPath), mainImageId), thumbnailId)) =>
          AsServedImageDescriptor(ImageDescriptor(mainImageId, mainImagePath), ImageDescriptor(thumbnailId, thumbnailPath))
      }
    }

  def processForGuideImageBase(sourceImageId: Long): Either[ImageServiceOrDatabaseError, ImageDescriptor] = {
    ???
  }

  def processForGuideImageOverlays(sourceImageId: Long): Either[ImageServiceOrDatabaseError, Seq[ImageDescriptor]] = {
    ???
  }

  def processForSelectionScreen(pathPrefix: String, sourceImageId: Long): Either[ImageServiceOrDatabaseError, ImageDescriptor] =
    for (sourceDescriptor <- wrapDatabaseError(imageDatabase.getSourceImageDescriptors(Seq(sourceImageId))).right;
         actualPath <- processAndUploadSelectionScreenImage(pathPrefix, sourceDescriptor.head).right;
         ssiId <- wrapDatabaseError(imageDatabase.createProcessedImageRecords(Seq(ProcessedImageRecord(actualPath, sourceDescriptor.head.id, ProcessedImagePurpose.PortionSizeSelectionImage)))).right)
      yield ImageDescriptor(ssiId.head, actualPath)

}