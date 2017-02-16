package uk.ac.ncl.openlab.intake24.foodsql.images

import java.time.LocalDateTime
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm.NamedParameter.symbol
import anorm.{AnormUtil, Macro, NamedParameter, SQL, sqlToSimple}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

class ImageDatabaseServiceSqlImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends ImageDatabaseService with SqlDataService with SqlResourceLoader {

  private val logger = LoggerFactory.getLogger(classOf[ImageDatabaseServiceSqlImpl])

  private case class SourceImageRecordRow(id: Long, path: String, thumbnail_path: String, keywords: Array[String], uploader: String, uploaded_at: LocalDateTime) {
    def toSourceImageRecord = SourceImageRecord(id, path, thumbnail_path, keywords, uploader, uploaded_at)
  }

  private lazy val listSourceImagesQuery = sqlFromResource("admin/list_source_image_records.sql")

  private lazy val getSourceImagesQuery = sqlFromResource("admin/get_source_image_records.sql")

  private lazy val filterSourceImagesQuery = sqlFromResource("admin/filter_source_image_records.sql")

  def createSourceImageRecords(records: Seq[NewSourceImageRecord]): Either[UnexpectedDatabaseError, Seq[Long]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val query = "INSERT INTO source_images(id,path,thumbnail_path,uploader,uploaded_at) VALUES(DEFAULT,{path},{thumbnail_path},{uploader},DEFAULT)"

        val params = records.map {
          rec => Seq[NamedParameter]('path -> rec.path, 'thumbnail_path -> rec.thumbnailPath, 'uploader -> rec.uploader)
        }

        val ids = AnormUtil.batchKeys(batchSql(query, params))

        val keywordParams = records.zip(ids).flatMap {
          case (r, id) =>
            r.keywords.map {
              keyword =>
                Seq[NamedParameter]('id -> id, 'keyword -> keyword)
            }
        }

        val keywordsQuery = "INSERT INTO source_image_keywords VALUES ({id},{keyword})"

        if (keywordParams.nonEmpty)
          batchSql(keywordsQuery, keywordParams).execute()

        Right(ids)
      }
  }

  def getSourceImageRecords(ids: Seq[Long]): Either[LookupError, Seq[SourceImageRecord]] = tryWithConnection {
    implicit conn =>

      val result = SQL(getSourceImagesQuery).on('ids -> ids).executeQuery().as(Macro.namedParser[SourceImageRecordRow].*).map {
        row =>
          row.id -> row.toSourceImageRecord
      }.toMap

      ids.find(!result.contains(_)) match {
        case Some(missingKey) => Left(RecordNotFound(new RuntimeException(s"Missing source image record: $missingKey")))
        case None => Right(ids.map(result(_)))
      }
  }


  def listSourceImageRecords(offset: Int, limit: Int, search: Option[String]): Either[UnexpectedDatabaseError, Seq[SourceImageRecord]] = tryWithConnection {
    implicit conn =>
      val query = search match {
        case Some(term) if term.nonEmpty => SQL(filterSourceImagesQuery).on('offset -> offset, 'limit -> limit, 'pattern -> s"${term.toLowerCase}%")
        case _ => SQL(listSourceImagesQuery).on('offset -> offset, 'limit -> limit)
      }

      val result = query.as(Macro.namedParser[SourceImageRecordRow].*).map(_.toSourceImageRecord)

      Right(result)
  }

  def updateSourceImageRecord(id: Long, update: SourceImageRecordUpdate): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("DELETE FROM source_image_keywords WHERE source_image_id={id}").on('id -> id).execute()

        val keywordParams = update.keywords.map {
          keyword =>
            Seq[NamedParameter]('id -> id, 'keyword -> keyword)
        }

        tryWithConstraintCheck[LookupError, Unit]("source_image_keywords_source_image_id_fk", e => RecordNotFound(e)) {
          batchSql("INSERT INTO source_image_keywords VALUES({id},{keyword})", keywordParams).execute()
          Right(())
        }
      }
  }

  override def deleteSourceImageRecords(ids: Seq[Long]): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        tryWithConstraintCheck[DeleteError, Unit]("processed_images_source_image_fk", e => StillReferenced(e)) {
          if (SQL("DELETE FROM source_images WHERE id IN ({ids})").on('ids -> ids).executeUpdate() == ids.length)
            Right(())
          else
            Left(RecordNotFound(new RuntimeException("One of the ids does not exist")))
        }
      }
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