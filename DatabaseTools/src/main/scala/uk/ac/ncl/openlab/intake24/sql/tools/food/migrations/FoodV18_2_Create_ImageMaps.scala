package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path, Paths}
import java.util.function.BiPredicate

import anorm.{AnormUtil, BatchSql, Macro, NamedParameter, SQL}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{AWTImageMap, ProcessedImagePurpose, SVGImageMapParser}
import uk.ac.ncl.openlab.intake24.sql.tools._
import upickle.default._

import scala.collection.JavaConverters._

object FoodV18_2_Create_ImageMaps extends App with MigrationRunner with WarningMessage {

  private case class GuideImageRow(id: String, base_image_url: String)

  private case class GuideObjectRow(guide_image_id: String, object_id: Long)

  val svgParser = new SVGImageMapParser()

  trait Options extends ScallopConf with DatabaseConfigurationOptions {

    val imageMapsDir = opt[String](required = true, noshort = true)
    val compiledImageMapsDir = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  private def getImageMapFromSVG(id: String): Either[Throwable, AWTImageMap] = {

    println(s"Trying to locate source SVG for $id")

    val fileName = s"$id.svg"

    val predicate = new BiPredicate[Path, BasicFileAttributes] {
      override def test(t: Path, u: BasicFileAttributes): Boolean = t.getFileName.toString == fileName
    }

    val svg = Files.find(Paths.get(options.imageMapsDir()), 10, predicate).findFirst().get()

    val path = svg.toString
    println(s"Loading image map from $path")

    svgParser.parseImageMap(path)
  }

  private def getLegacyImageMapList(): Seq[String] = {
    val predicate = new BiPredicate[Path, BasicFileAttributes] {
      override def test(t: Path, u: BasicFileAttributes): Boolean = t.getFileName.toString.endsWith(".imagemap")
    }

    Files.find(Paths.get(options.compiledImageMapsDir()), 1, predicate).iterator().asScala.toSeq.map(_.getFileName.toString.replace(".imagemap", ""))
  }

  private case class GuideImageObjectRow(guide_image_id: String, object_id: Int, description: String)

  runMigration(18, 18, options) {
    implicit conn =>

      println("Loading legacy image map descriptions")

      val objectDescriptions = SQL("SELECT guide_image_id, object_id, description FROM guide_image_objects").executeQuery().as(Macro.namedParser[GuideImageObjectRow].*).foldLeft(Map[(String, Int), String]()) {
        (acc, row) =>
          acc + ((row.guide_image_id, row.object_id) -> row.description)
      }

      println("Loading legacy image map names")

      val imageMapIds = getLegacyImageMapList()

      imageMapIds.foreach {
        imageMapId =>
          println (s"Processing $imageMapId")
          val imageMap = getImageMapFromSVG(imageMapId)
      }
  }
}
