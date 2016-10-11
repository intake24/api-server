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

object AsServedToV4 extends App with WarningMessage with DatabaseConnection {
  private case class AsServedImageRow(id: Long, as_served_set_id: String, url: String)

  private case class RemappedAsServedImage(id: Long, set_id: String, sourcePath: String, mainImagePath: String, thumbnailPath: String)

  def copySource(id: String, sourceImageDir: String, imageDir: String, imagePath: String, targetDir: String, logger: Logger): String = {

    val name = FilenameUtils.getName(imagePath)

    logger.info(s"Trying to locate source file for $imagePath")

    val matcher = new BiPredicate[Path, BasicFileAttributes] {
      def test(path: Path, attr: BasicFileAttributes) = path.getFileName().toString().equals(name)
    }

    val file = Files.find(Paths.get(sourceImageDir), 20, matcher).findFirst()

    val dstRelativePath = s"source/as_served/$id/${FilenameUtils.getName(imagePath)}"
    val dstPath = Paths.get(targetDir).resolve(dstRelativePath)
    dstPath.toFile().getParentFile().mkdirs()

    val sourcePath = if (file.isPresent()) {
      val result = file.get
      logger.info(s"Found high-res source: ${result.toString()}")
      result
    } else {
      val result = Paths.get(imageDir).resolve(imagePath)
      logger.info(s"No high-res source available, using low-res: ${result.toString()}")
      result
    }

    logger.info(s"Copying source image ${sourcePath.toString()} to ${dstPath.toString()}")

    Files.copy(sourcePath, dstPath)

    dstRelativePath
  }

  def copyMain(imageDir: String, targetDir: String, id: String, imagePath: String, logger: Logger): String = {

    val dstName = s"as_served/$id/${FilenameUtils.getName(imagePath)}"

    val srcPath = Paths.get(imageDir).resolve(imagePath)
    val dstPath = Paths.get(targetDir).resolve(dstName)

    logger.info(s"Copying main image ${srcPath.toString()} to ${dstPath.toString()}")

    dstPath.toFile().getParentFile().mkdirs()

    Files.copy(srcPath, dstPath)

    dstName
  }

  def copyThumb(imageDir: String, targetDir: String, id: String, imagePath: String, logger: Logger): String = {

    val dstName = s"as_served/$id/thumbnails/${FilenameUtils.getName(imagePath)}"

    val srcPath = Paths.get(imageDir).resolve(s"Thumbnails/$imagePath")
    val dstPath = Paths.get(targetDir).resolve(dstName)

    logger.info(s"Copying thumbnail image ${srcPath.toString()} to ${dstPath.toString()}")

    dstPath.toFile().getParentFile().mkdirs()

    Files.copy(srcPath, dstPath)

    dstName
  }

  trait Options extends ScallopConf {
    version("Intake24 v3 as served tables data tool")

    val imageDir = opt[String](required = true, noshort = true)
    val sourceImageDir = opt[String](required = true, noshort = true)
    val targetImageDir = opt[String](required = true, noshort = true)
  }

  val versionFrom = 3l
  val versionTo = 4l

  val logger = LoggerFactory.getLogger(getClass)

  displayWarningMessage("WARNING: THIS OPERATION IS DESTRUCTIVE AND CANNOT BE UNDONE!")

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  val dataSource = getDataSource(options)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  if (version != versionFrom) {
    logger.error(s"Wrong schema version: expected $versionFrom, got $version")
  } else {

    val rows = SQL("SELECT id, as_served_set_id, url FROM as_served_images ORDER BY id").executeQuery().as(Macro.namedParser[AsServedImageRow].*)

    val remapped = rows.map {
      row =>
        RemappedAsServedImage(row.id, row.as_served_set_id,
          copySource(row.as_served_set_id, options.sourceImageDir(), options.imageDir(), row.url, options.targetImageDir(), logger),
          copyMain(options.imageDir(), options.targetImageDir(), row.as_served_set_id, row.url, logger),
          copyThumb(options.imageDir(), options.targetImageDir(), row.as_served_set_id, row.url, logger))
    }

    val availableSource = remapped.map(_.sourcePath)

    val sourceParams = availableSource.map {
      path =>
        Seq[NamedParameter]('path -> path, 'keywords -> "", 'uploader -> "admin#")
    }

    val keys = AnormUtil.batchKeys(BatchSql("INSERT INTO source_images VALUES (DEFAULT,{path},{keywords},{uploader},DEFAULT)", sourceParams))

    val sourceIdMap = availableSource.zip(keys).toMap

    val processedMainParams = remapped.map {
      r =>
        Seq[NamedParameter]('path -> r.mainImagePath, 'source_id -> sourceIdMap(r.sourcePath), 'purpose -> 1l)
    }

    val processedMainKeys = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedMainParams))

    val processedThumbParams = remapped.map {
      r =>
        Seq[NamedParameter]('path -> r.thumbnailPath, 'source_id -> sourceIdMap(r.sourcePath), 'purpose -> 2l)
    }
    
    val mainIdMap = remapped.map(_.mainImagePath).zip(processedMainKeys).toMap

    val processedThumbKeys = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedThumbParams))
    
    val thumbIdMap = remapped.map(_.thumbnailPath).zip(processedThumbKeys).toMap
    
    val asServedImageParams = remapped.map {
      r =>
        Seq[NamedParameter]('as_served_image_id -> r.id, 'image_id -> mainIdMap(r.mainImagePath), 'thumbnail_image_id -> thumbIdMap(r.thumbnailPath))
    }
    
    BatchSql("UPDATE as_served_images SET image_id={image_id},thumbnail_image_id={thumbnail_image_id} WHERE id={as_served_image_id}", asServedImageParams).execute()
    
    SQL("UPDATE schema_version SET version={version}").on('version -> versionTo).execute()
  }
}