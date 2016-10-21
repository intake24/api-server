package uk.ac.ncl.openlab.intake24.foodsql.images

import scala.Left
import scala.Right

import anorm.AnormUtil
import anorm.Macro
import anorm.NamedParameter
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import javax.inject.Inject
import javax.inject.Named
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDatabaseService
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDescriptor
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ProcessedImagePurpose
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ProcessedImageRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.images.SourceImageRecord
import org.slf4j.LoggerFactory
import org.joda.time.DateTime

class ImageDatabaseServiceSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends ImageDatabaseService with FoodDataSqlService {
  
  private val logger = LoggerFactory.getLogger(classOf[ImageDatabaseServiceSqlImpl])

  private case class SourceImageDescriptorRow(id: Long, path: String)

  def createSourceImageRecords(records: Seq[SourceImageRecord]): Either[DatabaseError, Seq[Long]] = tryWithConnection {
    implicit conn =>
      val query = "INSERT INTO source_images VALUES(DEFAULT,{path},{keywords},{uploader},DEFAULT)"

      val params = records.map {
        rec => Seq[NamedParameter]('path -> rec.path, 'keywords -> rec.keywords.map(_.toLowerCase()).toArray, 'uploader -> rec.uploader)
      }
      
      val result = AnormUtil.batchKeys(batchSql(query, params))
      
      Right(result)
  }

  def createProcessedImageRecords(records: Seq[ProcessedImageRecord]): Either[DatabaseError, Seq[Long]] = tryWithConnection {
    implicit conn =>
      val query = "INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)"

      val params = records.map {
        rec =>

          val purpose = rec.purpose match {
            case ProcessedImagePurpose.AsServedMainImage => 1l
            case ProcessedImagePurpose.AsServedThumbnail => 2l
          }

          Seq[NamedParameter]('path -> rec.path, 'source_id -> rec.sourceId, 'purpose -> purpose)
      }

      Right(AnormUtil.batchKeys(batchSql(query, params)))
  }

  def getSourceImageDescriptors(ids: Seq[Long]): Either[LookupError, Seq[ImageDescriptor]] = tryWithConnection {
    implicit conn =>

      val result = SQL("SELECT id, path FROM source_images WHERE id IN({ids})").on('ids -> ids).executeQuery().as(Macro.namedParser[SourceImageDescriptorRow].*).map {
        row =>
          row.id -> ImageDescriptor(row.id, row.path)
      }.toMap

      ids.find(!result.contains(_)) match {
        case Some(missingKey) => Left(RecordNotFound(new RuntimeException(s"Missing source record: $missingKey")))
        case None => Right(ids.map(result(_)))
      }
  }
}