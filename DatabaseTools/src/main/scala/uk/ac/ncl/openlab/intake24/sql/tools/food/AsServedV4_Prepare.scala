package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate

import scala.collection.mutable.Buffer

import org.apache.commons.io.FilenameUtils
import org.rogach.scallop.ScallopConf
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import anorm.Macro
import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import upickle.default.SeqishW
import upickle.default.write

object AsServedV4_Prepare extends App with WarningMessage with DatabaseConnection {
  private case class AsServedImageRow(as_served_set_id: String, url: String)

  private case class RemappedAsServedImage(set_id: String, url: String, sourcePath: String, mainImagePath: String, thumbnailPath: String)

  val cleanUp = Buffer[Path]()

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

    cleanUp.append(srcPath)

    dstName
  }

  def copyThumb(imageDir: String, targetDir: String, id: String, imagePath: String, logger: Logger): String = {

    val dstName = s"as_served/$id/thumbnails/${FilenameUtils.getName(imagePath)}"

    val srcPath = Paths.get(imageDir).resolve(s"Thumbnails/$imagePath")
    val dstPath = Paths.get(targetDir).resolve(dstName)

    logger.info(s"Copying thumbnail image ${srcPath.toString()} to ${dstPath.toString()}")

    dstPath.toFile().getParentFile().mkdirs()

    Files.copy(srcPath, dstPath)

    cleanUp.append(srcPath)

    dstName
  }

  trait Options extends ScallopConf {
    version("Intake24 v4 as served prepare")

    val imageDir = opt[String](required = true, noshort = true)
    val sourceImageDir = opt[String](required = true, noshort = true)
    val targetImageDir = opt[String](required = true, noshort = true)
    val remappingFile = opt[String](required = true, noshort = true)
  }

  val versionFrom = 3l

  val logger = LoggerFactory.getLogger(getClass)

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  val dataSource = getDataSource(options)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  if (version != versionFrom) {
    logger.error(s"Wrong schema version: expected $versionFrom, got $version")
  } else {

    val rows = SQL("SELECT as_served_set_id, url FROM as_served_images ORDER BY id").executeQuery().as(Macro.namedParser[AsServedImageRow].*)

    val remapped = rows.map {
      row =>
        RemappedAsServedImage(row.as_served_set_id, row.url,
          copySource(row.as_served_set_id, options.sourceImageDir(), options.imageDir(), row.url, options.targetImageDir(), logger),
          copyMain(options.imageDir(), options.targetImageDir(), row.as_served_set_id, row.url, logger),
          copyThumb(options.imageDir(), options.targetImageDir(), row.as_served_set_id, row.url, logger))
    }

    val writer = Files.newBufferedWriter(Paths.get(options.remappingFile()), Charset.forName("utf-8"))

    writer.write(write(remapped))

    writer.close()

    cleanUp.foreach {
      path =>
        if (Files.exists(path))
          Files.delete(path)
    }
  }
}