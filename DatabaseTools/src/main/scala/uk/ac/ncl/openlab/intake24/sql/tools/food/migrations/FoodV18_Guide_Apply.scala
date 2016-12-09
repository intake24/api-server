package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path, Paths}
import java.util.function.BiPredicate

import anorm.{AnormUtil, BatchSql, NamedParameter, SQL}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{ProcessedImagePurpose, SVGImageMapParser}
import uk.ac.ncl.openlab.intake24.sql.tools._
import upickle.default._

import scala.collection.JavaConverters._

object FoodV18_Guide_Apply extends App with MigrationRunner with WarningMessage {

  private case class GuideImageRow(id: String, base_image_url: String)

  private case class GuideObjectRow(guide_image_id: String, object_id: Long)

  val svgParser = new SVGImageMapParser()

  trait Options extends ScallopConf with DatabaseConfigurationOptions {

    val imageMapsDir = opt[String](required = true, noshort = true)
    val remappingFile = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  private def getImageMapFromSVG(id: String) = {

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

  runMigration(18, 19, options) {
    implicit conn =>

      val remapped = read[Seq[V18_RemappedGuideImageWithThumbs]](Files.readAllLines(Paths.get(options.remappingFile())).asScala.mkString)

      val sourceParams = remapped.map {
        m =>
          Seq[NamedParameter]('path -> m.sourcePath, 'uploader -> "admin", 'thumbnail_path -> m.sourceThumbnailPath)
      }

      println("Creating source image records")

      val sourceImageIds = AnormUtil.batchKeys(BatchSql("INSERT INTO source_images VALUES (DEFAULT,{path},{uploader},DEFAULT, {thumbnail_path})", sourceParams.head, sourceParams.tail: _*))

      val processedMainParams = remapped.zip(sourceImageIds).map {
        case (r, sourceKey) =>
          Seq[NamedParameter]('path -> r.mainImagePath, 'source_id -> sourceKey, 'purpose -> ProcessedImagePurpose.toId(ProcessedImagePurpose.GuideMainImage))
      }

      println("Creating processed image records for base images")

      val processedBaseImageIds = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedMainParams.head, processedMainParams.tail: _*))

      val processedSelectionParams = remapped.zip(sourceImageIds).map {
        case (r, sourceKey) =>
          Seq[NamedParameter]('path -> r.selectionImagePath, 'source_id -> sourceKey, 'purpose -> ProcessedImagePurpose.toId(ProcessedImagePurpose.PortionSizeSelectionImage))
      }

      println("Creating processed image records for selection screen images")

      val processedSelectionImageIds = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedSelectionParams.head, processedSelectionParams.tail: _*))

      remapped.zip(sourceImageIds).zip(processedBaseImageIds).zip(processedSelectionImageIds).foreach {
        case (((r, sourceImageId), processedBaseImageId), processedSelectionImageId) =>

          val processedOverlayParams = r.overlayPaths.map {
            case (_, path) =>
              Seq[NamedParameter]('path -> path, 'source_id -> sourceImageId, 'purpose -> ProcessedImagePurpose.toId(ProcessedImagePurpose.GuideOverlay))
          }

          println(s"Creating processed image records for overlays for ${r.id}")

          val processedOverlayIds = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedOverlayParams.head, processedOverlayParams.tail: _*)).zip(r.overlayPaths).map {
            case (id, (objectId, _)) => objectId -> id
          }.toMap

          println(s"Creating image map for ${r.id}")

          SQL("INSERT INTO image_maps VALUES({id},(SELECT description FROM guide_images WHERE id={id}),{base_image_id})").on('id -> r.id, 'base_image_id -> processedBaseImageId).execute()

          val imageMap = getImageMapFromSVG(r.id)

          val imageMapObjectParams = imageMap.outlines.keySet.toSeq.sorted.map {
            case objectId =>
              val navIndex = imageMap.navigation.indexOf(objectId)
              val podgon = s"{${imageMap.getCoordsArray(objectId).mkString(",")}}"
              Seq[NamedParameter]('id -> objectId.toLong, 'image_map_id -> r.id, 'navigation_index -> navIndex, 'outline_coordinates -> podgon, 'overlay_image_id -> processedOverlayIds(objectId))
          }

          println(s"Create image map objects for ${r.id}")

          BatchSql("INSERT INTO image_map_objects VALUES({id},{image_map_id},COALESCE((SELECT description FROM guide_image_objects WHERE guide_image_id={image_map_id} AND object_id={id}), 'No description'),{navigation_index},{outline_coordinates}::double precision[],{overlay_image_id})", imageMapObjectParams.head, imageMapObjectParams.tail: _*).execute()

          val guideObjectParams = r.overlayPaths.map {
            case (objectId, _) =>
              Seq[NamedParameter]('image_map_id -> r.id, 'guide_image_id -> r.id, 'object_id -> objectId)
          }

          println(s"Updating guide_image_objects image map and object ids")

          BatchSql("UPDATE guide_image_objects SET image_map_object_id=object_id, image_map_id={image_map_id} WHERE guide_image_id={guide_image_id} AND object_id={object_id}", guideObjectParams.head, guideObjectParams.tail: _*).execute()

          println (s"Updating guide_images with image map and selection screen ids")

          SQL("UPDATE guide_images SET image_map_id={image_map_id},selection_image_id={selection_image_id} WHERE id={guide_id}").on('guide_id -> r.id, 'image_map_id -> r.id, 'selection_image_id -> processedSelectionImageId).execute()
      }
  }
}
