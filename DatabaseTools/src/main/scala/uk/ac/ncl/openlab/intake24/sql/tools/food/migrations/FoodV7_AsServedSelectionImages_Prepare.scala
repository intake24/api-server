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

object FoodV7_AsServedSelectionImages_Prepare extends App with WarningMessage with DatabaseConnection {

  trait Options extends ScallopConf {
    version("Intake24 v8 as served prepare")

    val imageDir = opt[String](required = true, noshort = true)
    val remappingFile = opt[String](required = true, noshort = true)
  }

  val versionFrom = 7l

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.verify()

  val dataSource = getDataSource(options)

  implicit val dbConn = dataSource.getConnection

  val version = SQL("SELECT version FROM schema_version").executeQuery().as(SqlParser.long("version").single)

  if (version != versionFrom) {
    println(s"Wrong schema version: expected $versionFrom, got $version")
  } else {

    val sets = SQL("SELECT id FROM as_served_sets ORDER BY id").executeQuery().as(SqlParser.str("id").*)

    var imageMap = Map[String, ImagePaths]()

    sets.foreach {
      setId =>
        println(s"Processing $setId")

        val rows = SQL("""|SELECT source_images.path
                          |  FROM as_served_images
                          |    JOIN processed_images ON as_served_images.image_id=processed_images.id
                          |    JOIN source_images ON processed_images.source_id=source_images.id
                          |WHERE as_served_images.as_served_set_id={as_served_set_id}
                          |ORDER BY as_served_images.weight ASC""".stripMargin).on('as_served_set_id -> setId).executeQuery().as(SqlParser.str("path").+)

        val selectionImageSourcePath = rows(rows.length / 2)

        val extension = "." + FilenameUtils.getExtension(selectionImageSourcePath)

        val randomName = UUID.randomUUID().toString() + extension

        val srcPath = selectionImageSourcePath

        val dstPath = s"as_served/$setId/selection/$randomName"

        val srcFsPath = options.imageDir() + "/" + srcPath

        val dstFsPath = options.imageDir() + "/" + dstPath

        new File(dstFsPath).getParentFile.mkdirs()

        println(s"Using $srcFsPath as source")

        val cmd = new ConvertCmd()
        val op = new IMOperation()

        op.resize(300)
        op.background("white")
        op.gravity("Center")
        op.extent(300, 200)
        op.addImage(srcFsPath)
        op.addImage(dstFsPath)

        println(s"Invoking ImageMagick: ${((cmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")

        cmd.run(op)

        imageMap += (setId -> ImagePaths(srcPath, dstPath))
    }

    val writer = Files.newBufferedWriter(Paths.get(options.remappingFile()), Charset.forName("utf-8"))

    writer.write(write(imageMap))

    writer.close()
  }
}
