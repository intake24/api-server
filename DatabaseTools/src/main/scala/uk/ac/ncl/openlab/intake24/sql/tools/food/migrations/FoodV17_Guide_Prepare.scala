package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.charset.Charset
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path, Paths}
import java.util.function.BiPredicate

import anorm.{Macro, SqlParser, _}
import org.apache.commons.io.FilenameUtils
import org.rogach.scallop.ScallopConf
import org.slf4j.{Logger, LoggerFactory}
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, DatabaseOptions, WarningMessage}
import upickle.default._

import scala.collection.mutable

case class V17_RemappedGuideImage(id: String, url: String, sourcePath: String, mainImagePath: String, overlayPaths: Seq[(Long, String)])

object FoodV17_Guide_Prepare extends App with WarningMessage with DatabaseConnection {

  private case class GuideImageRow(id: String, base_image_url: String)

  private case class GuideObjectRow(guide_image_id: String, object_id: Long)

  val cleanUp = mutable.Buffer[Path]()

  def copySource(id: String, sourceImageDir: String, imageDir: String, imagePath: String, targetDir: String): String = {

    val name = FilenameUtils.getName(imagePath)

    println(s"Trying to locate source file for $imagePath")

    val matcher = new BiPredicate[Path, BasicFileAttributes] {
      def test(path: Path, attr: BasicFileAttributes) = path.getFileName().toString().equals(name)
    }

    val file = Files.find(Paths.get(sourceImageDir), 20, matcher).findFirst()

    val dstRelativePath = s"source/guide/$id/${FilenameUtils.getName(imagePath)}"
    val dstPath = Paths.get(targetDir).resolve(dstRelativePath)
    dstPath.toFile().getParentFile().mkdirs()

    val sourcePath = if (file.isPresent()) {
      val result = file.get
      println(s"Found high-res source: ${result.toString()}")
      result
    } else {
      val result = Paths.get(imageDir).resolve(imagePath)
      println(s"No high-res source available, using low-res: ${result.toString()}")
      result
    }

    println(s"Copying source image ${sourcePath.toString()} to ${dstPath.toString()}")

    Files.copy(sourcePath, dstPath)

    dstRelativePath
  }

  def copyMain(imageDir: String, targetDir: String, id: String, imagePath: String): String = {

    val dstName = s"guide/$id/${FilenameUtils.getName(imagePath)}"

    val srcPath = Paths.get(imageDir).resolve(imagePath)
    val dstPath = Paths.get(targetDir).resolve(dstName)

    println(s"Copying base image ${srcPath.toString()} to ${dstPath.toString()}")

    dstPath.toFile().getParentFile().mkdirs()

    Files.copy(srcPath, dstPath)

    cleanUp.append(srcPath)

    dstName
  }

  def copyOverlays(imageDir: String, targetDir: String, id: String, objectIds: Seq[Long], dummyPngPath: Path): Seq[(Long, String)] =
    objectIds.map {
      objectId =>

        val dstName = s"guide/$id/overlays/$objectId.png"

        val srcPath = Paths.get(imageDir).resolve(s"Overlays/$id/$objectId.png")
        val dstPath = Paths.get(targetDir).resolve(dstName)

        println(s"Copying overlay image ${srcPath.toString()} to ${dstPath.toString()}")

        dstPath.toFile().getParentFile().mkdirs()

        if (Files.exists(srcPath)) {
          Files.copy(srcPath, dstPath)
          cleanUp.append(srcPath)
        } else {
          println(s"Source file ${srcPath.toString} does not exist! Using dummy png instead.")
          Files.copy(dummyPngPath, dstPath)
        }

        (objectId, dstName)
    }

  trait Options extends ScallopConf with DatabaseConfigurationOptions {

    val imageDir = opt[String](required = true, noshort = true)
    val sourceImageDir = opt[String](required = true, noshort = true)
    val targetImageDir = opt[String](required = true, noshort = true)
    val remappingFile = opt[String](required = true, noshort = true)
    val dummyPngPath = opt[String](required = true, noshort = true)
  }

  val versionFrom = 17l

  val options = new ScallopConf(args) with Options

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  val dataSource = getDataSource(dbConfig)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  if (version != versionFrom) {
    throw new RuntimeException(s"Wrong schema version: expected $versionFrom, got $version")
  } else {

    val rows = SQL("SELECT id, base_image_url FROM guide_images ORDER BY id").executeQuery().as(Macro.namedParser[GuideImageRow].*)

    val objectRows = SQL("SELECT guide_image_id, object_id FROM guide_image_objects").executeQuery().as(Macro.namedParser[GuideObjectRow].*).foldLeft(Map[String, Seq[Long]]()) {
      case (acc, row) => acc + (row.guide_image_id -> (row.object_id +: acc.getOrElse(row.guide_image_id, Seq())))
    }

    val remapped = rows.map {
      row =>
        V17_RemappedGuideImage(row.id, row.base_image_url,
          copySource(row.id, options.sourceImageDir(), options.imageDir(), row.base_image_url, options.targetImageDir()),
          copyMain(options.imageDir(), options.targetImageDir(), row.id, row.base_image_url),
          copyOverlays(options.imageDir(), options.targetImageDir(), row.id, objectRows(row.id), Paths.get(options.dummyPngPath())))
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
