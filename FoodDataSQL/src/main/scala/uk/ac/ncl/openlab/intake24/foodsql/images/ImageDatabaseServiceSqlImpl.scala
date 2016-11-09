package uk.ac.ncl.openlab.intake24.foodsql.images

import java.time.LocalDateTime
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm.NamedParameter.symbol
import anorm.{AnormUtil, Macro, NamedParameter, SQL, sqlToSimple}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.{LookupError, RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.fooddb.images._

class ImageDatabaseServiceSqlImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends ImageDatabaseService with FoodDataSqlService {

  private val logger = LoggerFactory.getLogger(classOf[ImageDatabaseServiceSqlImpl])

    private case class SourceImageDescriptorRow(id: Long, path: String)

  private case class SourceImageRecordRow(id: Long, path: String, thumbnail_path: String, keywords: Array[String], uploader: String, uploaded_at: LocalDateTime)

  def listSourceImageRecords(offset: Int, limit: Int): Either[UnexpectedDatabaseError, Seq[SourceImageRecord]] = tryWithConnection {
    implicit conn =>
      val query = "SELECT id, path, thumbnail_path, keywords, uploader, uploaded_at FROM source_images ORDER BY id OFFSET {offset} LIMIT {limit}"

      val result = SQL(query).on('offset -> offset, 'limit -> limit).as(Macro.namedParser[SourceImageRecordRow].*).map {
        row =>
          SourceImageRecord(row.id, row.path, row.thumbnail_path, row.keywords, row.uploader, row.uploaded_at)
      }

      Right(result)
  }

  def createSourceImageRecords(records: Seq[NewSourceImageRecord]): Either[UnexpectedDatabaseError, Seq[Long]] = tryWithConnection {
    implicit conn =>
      val query = "INSERT INTO source_images VALUES(DEFAULT,{path},{thumbnail_path},{keywords},{uploader},DEFAULT)"

      val params = records.map {
        rec => Seq[NamedParameter]('path -> rec.path, 'thumbnail_path -> rec.thumbnailPath, 'keywords -> rec.keywords.map(_.toLowerCase()).toArray, 'uploader -> rec.uploader)
      }

      val result = AnormUtil.batchKeys(batchSql(query, params))

      Right(result)
  }

  def createProcessedImageRecords(records: Seq[ProcessedImageRecord]): Either[UnexpectedDatabaseError, Seq[Long]] = tryWithConnection {
    implicit conn =>
      val query = "INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)"

      val params = records.map {
        rec =>
          Seq[NamedParameter]('path -> rec.path, 'source_id -> rec.sourceId, 'purpose -> ProcessedImagePurpose.toId(rec.purpose))
      }

      Right(AnormUtil.batchKeys(batchSql(query, params)))
  }

  def deleteProcessedImageRecords(ids: Seq[Long]): Either[UnexpectedDatabaseError, Unit] =
    tryWithConnection {
      implicit conn =>
        val query = "DELETE FROM processed_images WHERE id IN ({ids})"

        SQL(query).on('ids -> ids).execute()

        Right(())
    }

  def getSourceImageDescriptors(ids: Seq[Long]): Either[LookupError, Seq[ImageDescriptor]] = tryWithConnection {
    implicit conn =>

      val result = SQL("SELECT id, path FROM source_images WHERE id IN({ids})").on('ids -> ids).executeQuery().as(Macro.namedParser[SourceImageDescriptorRow].*).map {
        row =>
          row.id -> ImageDescriptor(row.id, row.path)
      }.toMap

      ids.find(!result.contains(_)) match {
        case Some(missingKey) => Left(RecordNotFound(new RuntimeException(s"Missing source image record: $missingKey")))
        case None => Right(ids.map(result(_)))
      }
  }

  private case class ProcessedImageRecordRow(id: Long, path: String, source_id: Long, purpose: Long)

  def getProcessedImageRecords(ids: Seq[Long]): Either[LookupError, Seq[ProcessedImageRecord]] = tryWithConnection {
    implicit conn =>
      val result = SQL("SELECT id, path, source_id, purpose FROM processed_images WHERE id IN({ids})").on('ids -> ids).executeQuery().as(Macro.namedParser[ProcessedImageRecordRow].*).map {
        row =>
          row.id -> ProcessedImageRecord(row.path, row.source_id, ProcessedImagePurpose.fromId(row.purpose))
      }.toMap

      ids.find(!result.contains(_)) match {
        case Some(missingKey) => Left(RecordNotFound(new RuntimeException(s"Missing processed image record: $missingKey")))
        case None => Right(ids.map(result(_)))
      }
  }
}