package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import javax.inject.Inject

import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory

class ImageAdminServiceDefaultImpl @Inject()(val imageDatabase: ImageDatabaseService, val imageProcessor: ImageProcessor, val storage: ImageStorageService,
                                             val fileTypeAnalyzer: FileTypeAnalyzer)
  extends ImageAdminService {

  import ImageAdminService.{WrapDatabaseError, WrapImageServiceError}

  private val allowedFileTypes = Seq("image/jpeg", "image/png", "image/svg+xml")

  private val sourcePathPrefix = "source"

  private val tempFilePrefix = "intake24-"

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
      path => (path, storage.deleteImage(path))
    }

    // Do not fail complex operations because of failed deletions, but still warn
    results.foreach {
      case (path, Left(err)) =>
        logger.warn(s"Could not delete $path", err.e)
      case _ => {}
    }

    Right(())
  }

  def deleteProcessedImages(ids: Seq[Int]): Either[ImageServiceOrDatabaseError, Unit] = {
    for (
      paths <- imageDatabase.getProcessedImageRecords(ids).wrapped.right.map(_.map(_.path)).right;
      _ <- imageDatabase.deleteProcessedImageRecords(ids).wrapped.right;
      _ <- deleteImagesFromStorageImpl(paths).wrapped.right
    ) yield ()
  }

  // TODO: if the database operation fails, images need to be deleted from storage.
  // Failing to do that won't break anything, but will result in unused files.
  // Maybe some garbage collection is a better idea?
  def uploadSourceImage(suggestedPath: String, source: Path, keywords: Seq[String], uploaderName: String): Either[ImageServiceOrDatabaseError, Long] =
  withTempDir {
    tempDir =>
      val extension = getExtension(source.toString)
      val thumbDst = Files.createTempFile(tempDir, tempFilePrefix, extension)

      for (
        _ <- {
          logger.debug("Checking file type")
          checkFileType(source).wrapped.right
        };
        _ <- {
          logger.debug("Generating fixed size thumbnail")
          imageProcessor.processForSourceThumbnail(source, thumbDst).wrapped.right
        };
        actualPath <- {
          logger.debug("Uploading source image to storage")
          storage.uploadImage(sourcePathPrefix + File.separator + suggestedPath, source).wrapped.right
        };
        actualThumbPath <- {
          logger.debug("Uploading thumbnail image to storage")
          storage.uploadImage(sourcePathPrefix + "/thumbnails/" + suggestedPath, thumbDst).wrapped.right
        };
        id <- {
          logger.debug("Creating a database record for the source image")
          imageDatabase.createSourceImageRecords(Seq(NewSourceImageRecord(actualPath, actualThumbPath, keywords, uploaderName))).wrapped.right
        }
      ) yield id.head
  }

  def deleteSourceImages(ids: Seq[Int]): Either[ImageServiceOrDatabaseError, Unit] = {
    for (
      records <- imageDatabase.getSourceImageRecords(ids).wrapped.right;
      _ <- imageDatabase.deleteSourceImageRecords(ids).wrapped.right;
      _ <- deleteImagesFromStorageImpl(records.map(_.path)).wrapped.right;
      _ <- deleteImagesFromStorageImpl(records.map(_.thumbnailPath)).wrapped.right
    ) yield ()
  }

  private case class AsServedImagePaths(mainImage: String, thumbnail: String)

  private def getExtension(path: String) = "." + FilenameUtils.getExtension(path).toLowerCase()

  private def processAndUploadAsServed(setId: String, sourcePaths: Seq[String]): Either[ImageServiceOrDatabaseError, Seq[AsServedImagePaths]] = {
    withTempDir {
      tempDir =>
        def rec(srcs: Seq[String], acc: Vector[AsServedImagePaths]): Either[ImageServiceOrDatabaseError, Vector[AsServedImagePaths]] =
          if (srcs.isEmpty)
            Right(acc)
          else {
            val path = srcs.head

            val extension = getExtension(path)

            val srcPath = Files.createTempFile(tempDir, tempFilePrefix, extension)
            val dstMainPath = Files.createTempFile(tempDir, tempFilePrefix, extension)
            val dstThumbnailPath = Files.createTempFile(tempDir, tempFilePrefix, extension)

            val randomName = UUID.randomUUID().toString() + extension

            val processResult = for (
              _ <- storage.downloadImage(path, srcPath).wrapped.right;
              _ <- imageProcessor.processForAsServed(srcPath, dstMainPath, dstThumbnailPath).wrapped.right;
              actualMainPath <- storage.uploadImage(s"as_served/$setId/$randomName", dstMainPath).wrapped.right;
              actualThumbPath <- storage.uploadImage(s"as_served/$setId/thumbnails/$randomName", dstThumbnailPath).wrapped.right
            ) yield AsServedImagePaths(actualMainPath, actualThumbPath)

            processResult match {
              case Right(paths) => rec(srcs.tail, acc :+ paths)
              case Left(error) => Left(error)
            }
          }

        rec(sourcePaths, Vector())
    }
  }

  private def processAndUploadImageMapBase(imageMapId: String, sourcePath: String): Either[ImageServiceOrDatabaseError, String] =
    withTempDir {
      tempDir =>
        val extension = getExtension(sourcePath)

        val srcPath = Files.createTempFile(tempDir, tempFilePrefix, extension)
        val dstPath = Files.createTempFile(tempDir, tempFilePrefix, extension)

        val randomName = UUID.randomUUID().toString() + extension

        for (
          _ <- storage.downloadImage(sourcePath, srcPath).wrapped.right;
          _ <- imageProcessor.processForImageMapBase(srcPath, dstPath).wrapped.right;
          actualPath <- storage.uploadImage(s"image_maps/$imageMapId/$randomName", dstPath).wrapped.right
        ) yield actualPath
    }

  private def generateAndUploadImageMapOverlays(imageMapId: String, imageMap: AWTImageMap): Either[ImageServiceOrDatabaseError, Map[Int, String]] =
    withTempDir {
      tempDir =>

        logger.debug(s"Generating overlays for image map $imageMapId")

        def upload(remaining: Map[Int, Path], acc: Map[Int, String] = Map()): Either[ImageServiceOrDatabaseError, Map[Int, String]] = remaining.headOption match {
          case Some((objectId, path)) =>
            val extension = getExtension(path.toString)
            val randomName = UUID.randomUUID().toString + extension
            storage.uploadImage(s"image_maps/$imageMapId/overlays/$randomName", path) match {
              case Left(e) => Left(ImageServiceErrorWrapper(e))
              case Right(actualPath) => upload(remaining - objectId, acc + (objectId -> actualPath))
            }
          case None => Right(acc)
        }

        imageProcessor.generateImageMapOverlays(imageMap, tempDir).wrapped.right.flatMap(upload(_))
    }

  private def processAndUploadSelectionScreenImage(pathPrefix: String, sourcePath: String): Either[ImageServiceOrDatabaseError, String] =
    withTempDir {
      tempDir =>
        val extension = getExtension(sourcePath)

        val srcPath = Files.createTempFile(tempDir, tempFilePrefix, extension)
        val dstPath = Files.createTempFile(tempDir, tempFilePrefix, extension)

        val randomName = UUID.randomUUID().toString() + extension

        for (
          _ <- storage.downloadImage(sourcePath, srcPath).wrapped.right;
          _ <- imageProcessor.processForSelectionScreen(srcPath, dstPath).wrapped.right;
          actualPath <- storage.uploadImage(s"$pathPrefix/$randomName", dstPath).wrapped.right
        ) yield actualPath
    }

  private def mkProcessedMainImageRecords(sourceIds: Seq[Int], paths: Seq[AsServedImagePaths]): Seq[ProcessedImageRecord] =
    sourceIds.zip(paths).map { case (id, paths) => ProcessedImageRecord(paths.mainImage, id, ProcessedImagePurpose.AsServedMainImage) }

  private def mkProcessedThumbnailRecords(sourceIds: Seq[Int], paths: Seq[AsServedImagePaths]): Seq[ProcessedImageRecord] =
    sourceIds.zip(paths).map { case (id, paths) => ProcessedImageRecord(paths.thumbnail, id, ProcessedImagePurpose.AsServedThumbnail) }

  def processForAsServed(setId: String, sourceImageIds: Seq[Int]): Either[ImageServiceOrDatabaseError, Seq[AsServedImageDescriptor]] =
    for (
      sources <- {
        logger.debug("Getting source image descriptors")
        imageDatabase.getSourceImageRecords(sourceImageIds).wrapped.right
      };
      uploadedPaths <- {
        logger.debug("Processing and uploading as served images")
        processAndUploadAsServed(setId, sources.map(_.path)).right
      };
      mainImageIds <- {
        val records = mkProcessedMainImageRecords(sourceImageIds, uploadedPaths)
        logger.debug(s"Creating processed image records for main images")
        records.foreach(r => logger.debug(r.toString()))
        imageDatabase.createProcessedImageRecords(records).wrapped.right
      };
      thumbnailIds <- {
        val records = mkProcessedThumbnailRecords(sourceImageIds, uploadedPaths)
        logger.debug("Creating processed image records for thumbnail images")
        records.foreach(r => logger.debug(r.toString()))
        imageDatabase.createProcessedImageRecords(records).wrapped.right
      }
    ) yield {
      uploadedPaths.zip(mainImageIds).zip(thumbnailIds).map {
        case (((AsServedImagePaths(mainImagePath, thumbnailPath), mainImageId), thumbnailId)) =>
          AsServedImageDescriptor(ImageDescriptor(mainImageId, mainImagePath), ImageDescriptor(thumbnailId, thumbnailPath))
      }
    }

  def processForImageMapBase(imageMapId: String, sourceImageId: Int): Either[ImageServiceOrDatabaseError, ImageDescriptor] = {
    logger.debug(s"Processing base image for image map $imageMapId")
    for (
      source <- {
        logger.debug("Getting source image descriptor")
        imageDatabase.getSourceImageRecords(Seq(sourceImageId)).wrapped.right.map(_.head).right
      };
      uploadedPath <- {
        logger.debug("Processing and uploading image")
        processAndUploadImageMapBase(imageMapId, source.path).right
      };
      baseImageId <- {
        val record = ProcessedImageRecord(uploadedPath, source.id, ProcessedImagePurpose.ImageMapBaseImage)
        logger.debug(s"Creating processed image record for the image")
        imageDatabase.createProcessedImageRecords(Seq(record)).wrapped.right.map(_.head).right
      }
    ) yield {
      ImageDescriptor(baseImageId, uploadedPath)
    }
  }

  def generateImageMapOverlays(imageMapId: String, sourceId: Int, imageMap: AWTImageMap): Either[ImageServiceOrDatabaseError, Map[Int, ImageDescriptor]] =
    generateAndUploadImageMapOverlays(imageMapId, imageMap).right.flatMap {
      overlays =>
        val (objectIds, overlayPaths) = overlays.toSeq.unzip
        val records = overlayPaths.map(path => ProcessedImageRecord(path, sourceId, ProcessedImagePurpose.ImageMapOverlay))

        imageDatabase.createProcessedImageRecords(records).wrapped.right.map {
          processedImageIds =>
            val descriptors = processedImageIds.zip(overlayPaths).map {
              case (id, path) => ImageDescriptor(id, path)
            }
            objectIds.zip(descriptors).toMap
        }
    }

  def processForSelectionScreen(pathPrefix: String, sourceImageId: Int): Either[ImageServiceOrDatabaseError, ImageDescriptor] =
    for (source <- imageDatabase.getSourceImageRecords(Seq(sourceImageId)).wrapped.right;
         actualPath <- processAndUploadSelectionScreenImage(pathPrefix, source.head.path).right;
         ssiId <- imageDatabase.createProcessedImageRecords(Seq(ProcessedImageRecord(actualPath, source.head.id, ProcessedImagePurpose.PortionSizeSelectionImage))).wrapped.right)
      yield ImageDescriptor(ssiId.head, actualPath)

}