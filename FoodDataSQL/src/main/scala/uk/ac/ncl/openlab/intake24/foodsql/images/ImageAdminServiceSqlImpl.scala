package uk.ac.ncl.openlab.intake24.foodsql.images

import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageServiceError
import uk.ac.ncl.openlab.intake24.services.fooddb.images.AsServedImageDescriptor
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageProcessor
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageService
import javax.inject.Inject
import java.io.File
import anorm.SQL
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.{ DatabaseError => DBE }
import uk.ac.ncl.openlab.intake24.services.fooddb.images.DatabaseError
import javax.sql.DataSource
import javax.inject.Named
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDescriptor
import anorm.SqlParser
import anorm.Macro
import org.apache.commons.io.FilenameUtils
import java.util.UUID
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageError

class ImageAdminServiceSqlImpl @Inject() @Named("intake24_foods") (val dataSource: DataSource, val imageProcessor: ImageProcessor, val storage: ImageStorageService) extends ImageAdminService with FoodDataSqlService {

  val PurposeAsServedMain = 1l
  val PurposeAsServedThumbnail = 2l

  private def mapError[T](result: Either[DBE, T]) = result.left.map {
    case DBE(e) => DatabaseError(e)
  }

  private def writeSourceImageDescriptor(path: String, keywords: Seq[String], uploader: String): Either[ImageServiceError, Long] = mapError(tryWithConnection {
    implicit conn =>
      val id = SQL("INSERT INTO source_images VALUES(DEFAULT,{path},{keywords},{uploader},DEFAULT)").on('path -> path, 'keywords -> keywords.mkString(" "),
        'uploader -> uploader).executeInsert()

      Right(id.get)
  })

  private def writeProcessedImageDescriptorQuery(path: String, sourceId: Long, purpose: Long)(implicit connection: java.sql.Connection): Either[Nothing, Long] = {
    val id = SQL("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)").on('path -> path, 'source_id -> sourceId, 'purpose -> purpose)
      .executeInsert()
    Right(id.get)
  }

  private case class SourceImageDescriptorRow(id: Long, relative_path: String)

  private def getSourceImageDescriptorsQuery(ids: Seq[Long])(implicit connection: java.sql.Connection): Either[Nothing, Map[Long, ImageDescriptor]] = {
    val result = SQL("SELECT id, relative_path FROM source_images WHERE id IN({ids})").executeQuery().as(Macro.namedParser[SourceImageDescriptorRow].*).map {
      row =>
        row.id -> ImageDescriptor(row.id, row.relative_path)
    }.toMap
    Right(result)
  }

  def uploadSourceImage(file: File, name: String, keywords: Seq[String], uploader: String): Either[ImageServiceError, Long] =
    for (
      actualPath <- storage.uploadImage("source" + File.pathSeparator + name, file).right;
      id <- writeSourceImageDescriptor(actualPath, keywords, uploader).right
    ) yield id

  
  private def downloadImages(paths: Map[Long, String]): Either[ImageStorageError, Map[Long, File]] = {
      val results = paths.foldLeft(Map[Long, Either[ImageStorageError, File]]()) { case (res, (id, path)) => res + (id -> storage.downloadImage(path)) }
      
/*
      
      results.find(_._2.isLeft) match {
        case Some((_, error)) => {
          // If any of the downloads failed, clean up everything and return the first error
          
          results.foreach {
            result => result._2 match {
              case Right(file) => file.delete()
            }
          }
          
          Left(error.left)
        }
        case None =>
          
      }
      
      if (results.exists(_._2.isLeft)) results.foreach {
        if (
      }*/
  }

  def processForAsServed(sourceImageIds: Seq[Long]): Either[ImageServiceError, Map[Long, AsServedImageDescriptor]] = mapError(tryWithConnection {
    implicit conn =>

      getSourceImageDescriptorsQuery(sourceImageIds).right.flatMap {
        sources =>

          val results = sources.foldLeft(Map[Long, Either[ImageServiceError, AsServedImageDescriptor]]()) {
            case (res, (id, desc)) =>

              val processResult = for (
                tmpSrc <- storage.downloadImage(desc.path).right;
                tmpProcessed <- imageProcessor.processForAsServed(tmpSrc).right;
                randomName <- { Right(UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(desc.path)) }.right;
                actualMainPath <- storage.uploadImage("as-served" + File.separator + randomName, tmpProcessed.mainImage).right;
                actualThumbPath <- storage.uploadImage("as-served" + File.separator + "thumbnails" + randomName, tmpProcessed.thumbnail).right;
                _ <- { tmpSrc.delete(); tmpProcessed.mainImage.delete(); tmpProcessed.thumbnail.delete(); Right(()) }.right;
                procMainImageId <- writeProcessedImageDescriptorQuery(actualMainPath, id, PurposeAsServedMain).right;
                procThumbnailId <- writeProcessedImageDescriptorQuery(actualThumbPath, id, PurposeAsServedThumbnail).right
              ) yield AsServedImageDescriptor(ImageDescriptor(procMainImageId, actualMainPath), ImageDescriptor(procThumbnailId, actualThumbPath))

              res + (id -> processResult)

          }

      }
  })

  def processForGuideImageBase(sourceImageId: Long): Either[ImageServiceError, ImageDescriptor] = {
    ???
  }

  def processForGuideImageOverlays(sourceImageId: Long): Either[ImageServiceError, Map[Int, ImageDescriptor]] = {
    ???
  }

  def uploadSourceImage(file: File, keywords: Seq[String]): Either[ImageServiceError, Long] = {
    ???
  }
}