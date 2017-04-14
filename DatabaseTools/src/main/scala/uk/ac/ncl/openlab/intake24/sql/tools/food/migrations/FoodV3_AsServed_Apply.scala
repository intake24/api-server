package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.file.{Files, Paths}

import anorm.{AnormUtil, BatchSql, NamedParameter, SqlParser, _}
import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConnection, DatabaseOptions, WarningMessage}
import upickle.default._
import scala.collection.JavaConverters._

object FoodV3_AsServed_Apply extends App with WarningMessage with DatabaseConnection {
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

    val keys = AnormUtil.batchKeys(BatchSql("INSERT INTO source_images VALUES (DEFAULT,{path},{keywords},{uploader},DEFAULT)", sourceParams.head, sourceParams.tail:_*))

    val processedMainParams = remapped.zip(keys).map {
      case (r, sourceKey) =>
        Seq[NamedParameter]('path -> r.mainImagePath, 'source_id -> sourceKey, 'purpose -> 1l)
    }

    logger.info("Creating processed image records for main images")

    val processedMainKeys = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedMainParams.head, processedMainParams.tail:_*))

    val processedThumbParams = remapped.zip(keys).map {
      case (r, sourceKey) =>
        Seq[NamedParameter]('path -> r.thumbnailPath, 'source_id -> sourceKey, 'purpose -> 2l)
    }

    logger.info("Creating processed image records for thumbnails")

    val processedThumbKeys = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedThumbParams.head, processedThumbParams.tail:_*))

    val asServedImageParams = remapped.zip(processedMainKeys).zip(processedThumbKeys).map {
      case ((r, mainImageKey), thumbKey) =>
        Seq[NamedParameter]('as_served_set_id -> r.set_id, 'url -> r.url, 'image_id -> mainImageKey, 'thumbnail_image_id -> thumbKey)
    }

    logger.info("Updating as served image records")

    BatchSql("UPDATE as_served_images SET image_id={image_id},thumbnail_image_id={thumbnail_image_id} WHERE as_served_set_id={as_served_set_id} AND url={url}", asServedImageParams.head, processedThumbParams.tail:_*).execute()

    SQL("UPDATE schema_version SET version={version}").on('version -> versionTo).execute()
  }
}
