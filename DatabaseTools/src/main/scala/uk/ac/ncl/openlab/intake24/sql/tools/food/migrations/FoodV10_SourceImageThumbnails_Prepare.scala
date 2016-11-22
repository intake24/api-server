package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import java.util.UUID

import anorm.{SqlParser, _}
import org.apache.commons.io.FilenameUtils
import org.im4java.core.{ConvertCmd, IMOperation}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConnection, DatabaseOptions, WarningMessage}
import upickle.default._
import scala.collection.JavaConverters._

object FoodV10_SourceImageThumbnails_Prepare extends App with WarningMessage with DatabaseConnection {

  trait Options extends ScallopConf {
    version("Intake24 food database generate source image thumbnails")

    val imageDir = opt[String](required = true, noshort = true)

    val remappingFile = opt[String](required = true, noshort = true)
  }

  val versionFrom = 10l

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.verify()

  val dataSource = getDataSource(options)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  if (version != versionFrom) {
    println(s"Wrong schema version: expected $versionFrom, got $version")
  } else {

    val sourcePaths = SQL("SELECT path FROM source_images").executeQuery().as(SqlParser.str("path").*)

    val thumbnailPaths = sourcePaths.map {
      sourcePath =>
        val extension = "." + FilenameUtils.getExtension(sourcePath)
        val randomName = UUID.randomUUID().toString() + extension

        val dstPath = FilenameUtils.getFullPathNoEndSeparator(sourcePath) + "/thumbnails/" + randomName

        val srcFsPath = options.imageDir() + "/" + sourcePath

        val dstFsPath = options.imageDir() + "/" + dstPath

        new File(dstFsPath).getParentFile.mkdirs()

        println(s"Using $srcFsPath as source")

        val cmd = new ConvertCmd()
        val op = new IMOperation()

        op.resize(768)
        op.background("white")
        op.gravity("Center")
        op.extent(768, 432)
        op.addImage(srcFsPath)
        op.addImage(dstFsPath)

        println(s"Invoking ImageMagick: ${((cmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")

        cmd.run(op)

        (sourcePath -> dstPath)

    }.toMap

    val writer = Files.newBufferedWriter(Paths.get(options.remappingFile()), Charset.forName("utf-8"))

    writer.write(write(thumbnailPaths))

    writer.close()
  }
}
