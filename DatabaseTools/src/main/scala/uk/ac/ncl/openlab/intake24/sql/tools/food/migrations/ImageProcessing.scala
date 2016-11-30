package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.io.File
import java.util.UUID

import org.apache.commons.io.FilenameUtils
import org.im4java.core.{ConvertCmd, IMOperation}

import scala.collection.JavaConverters._

trait ImageProcessing {

  def generateSourceImageThumbnails(sourceImagePaths: Seq[String], imageDir: String): Map[String, String] =
    sourceImagePaths.map {
      sourcePath =>
        val extension = "." + FilenameUtils.getExtension(sourcePath)
        val randomName = UUID.randomUUID().toString() + extension

        val dstPath = FilenameUtils.getFullPathNoEndSeparator(sourcePath) + "/thumbnails/" + randomName

        val srcFsPath = imageDir + "/" + sourcePath

        val dstFsPath = imageDir + "/" + dstPath

        new File(dstFsPath).getParentFile.mkdirs()

        println(s"Generating a thumbnail from $srcFsPath")

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

  def generateSelectionScreenImage(sourceImagePath: String, destPathPrefix: String, imageDir: String): String = {

    val extension = "." + FilenameUtils.getExtension(sourceImagePath)

    val randomName = UUID.randomUUID().toString() + extension

    val srcPath = sourceImagePath

    val dstPath = s"$destPathPrefix/selection/$randomName"

    val srcFsPath = imageDir + "/" + srcPath

    val dstFsPath = imageDir + "/" + dstPath

    new File(dstFsPath).getParentFile.mkdirs()

    println(s"Generating selection screen image from $srcFsPath")

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

    dstPath
  }
}
