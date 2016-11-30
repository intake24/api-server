package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools._
import upickle.default._

import scala.collection.JavaConverters._

case class V18_RemappedGuideImageWithThumbs(id: String, url: String, sourcePath: String, mainImagePath: String, overlayPaths: Seq[(Long, String)], sourceThumbnailPath: String, selectionImagePath: String)

object FoodV18_Guide_GenerateThumbs extends App with ImageProcessing {

  trait Options extends ScallopConf {

    val imageDir = opt[String](required = true, noshort = true)
    val remappingFileIn = opt[String](required = true, noshort = true)
    val remappingFileOut = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  val remapped = read[Seq[V18_RemappedGuideImage]](Files.readAllLines(Paths.get(options.remappingFileIn())).asScala.mkString)

  val thumbPaths = generateSourceImageThumbnails(remapped.map(_.sourcePath), options.imageDir())

  val remappedWithThumbs = remapped.map {
    remapped =>
      val selectionImagePath = generateSelectionScreenImage(remapped.sourcePath, s"guide/${remapped.id}", options.imageDir())
      V18_RemappedGuideImageWithThumbs(remapped.id, remapped.url, remapped.sourcePath, remapped.mainImagePath, remapped.overlayPaths, thumbPaths(remapped.sourcePath), selectionImagePath)
  }

  val writer = Files.newBufferedWriter(Paths.get(options.remappingFileOut()), Charset.forName("utf-8"))

  writer.write(write(remappedWithThumbs))

  writer.close()
}
