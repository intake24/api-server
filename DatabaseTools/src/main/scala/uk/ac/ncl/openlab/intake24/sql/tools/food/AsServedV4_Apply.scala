package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate

import org.apache.commons.io.FilenameUtils
import org.rogach.scallop.ScallopConf
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import anorm.AnormUtil
import anorm.BatchSql
import anorm.BatchSqlErrors
import anorm.Macro
import anorm.NamedParameter
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import scala.collection.mutable.Buffer
import scala.collection.JavaConverters._
import upickle.default._
object AsServedV4_Apply extends App with WarningMessage with DatabaseConnection {
  private case class AsServedImageRow(id: Long, as_served_set_id: String, url: String)

  private case class RemappedAsServedImage(set_id: String, url: String, sourcePath: String, mainImagePath: String, thumbnailPath: String)

  trait Options extends ScallopConf {
    version("Intake24 v4 as served apply")

    val remappingFile = opt[String](required = true, noshort = true)
  }

  val versionFrom = 3l
  val versionTo = 4l

  val logger = LoggerFactory.getLogger(getClass)

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.verify()

  val dataSource = getDataSource(options)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  if (version != versionFrom) {
    logger.error(s"Wrong schema version: expected $versionFrom, got $version")
  } else {

    val remapped = read[Seq[RemappedAsServedImage]](Files.readAllLines(Paths.get(options.remappingFile())).asScala.mkString)

    val sourceParams = remapped.map {
      m =>
        Seq[NamedParameter]('path -> m.sourcePath, 'keywords -> "", 'uploader -> "admin")
    }
    
    logger.info("Creating source image records")

    val keys = AnormUtil.batchKeys(BatchSql("INSERT INTO source_images VALUES (DEFAULT,{path},{keywords},{uploader},DEFAULT)", sourceParams))

    val processedMainParams = remapped.zip(keys).map {
      case (r, sourceKey) =>
        Seq[NamedParameter]('path -> r.mainImagePath, 'source_id -> sourceKey, 'purpose -> 1l)
    }
    
    logger.info("Creating processed image records for main images")

    val processedMainKeys = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedMainParams))

    val processedThumbParams = remapped.zip(keys).map {
      case (r, sourceKey) =>
        Seq[NamedParameter]('path -> r.thumbnailPath, 'source_id -> sourceKey, 'purpose -> 2l)
    }
    
    logger.info("Creating processed image records for thumbnails")

    val processedThumbKeys = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedThumbParams))

    val asServedImageParams = remapped.zip(processedMainKeys).zip(processedThumbKeys).map {
      case ((r, mainImageKey), thumbKey) =>
        Seq[NamedParameter]('as_served_set_id -> r.set_id, 'url -> r.url, 'image_id -> mainImageKey, 'thumbnail_image_id -> thumbKey)
    }
    
    logger.info("Updating as served image records")

    BatchSql("UPDATE as_served_images SET image_id={image_id},thumbnail_image_id={thumbnail_image_id} WHERE as_served_set_id={as_served_set_id} AND url={url}", asServedImageParams).execute()

    SQL("UPDATE schema_version SET version={version}").on('version -> versionTo).execute()
  }
}