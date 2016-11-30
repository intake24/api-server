package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.file.{Files, Paths}

import anorm.{AnormUtil, BatchSql, NamedParameter, SQL}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ProcessedImagePurpose
import uk.ac.ncl.openlab.intake24.sql.tools._
import upickle.default._


import scala.collection.JavaConverters._

object FoodV18_Guide_Apply extends App with MigrationRunner with WarningMessage {

  private case class GuideImageRow(id: String, base_image_url: String)

  private case class GuideObjectRow(guide_image_id: String, object_id: Long)

  private case class ImageMapArea(id: Int, coords: Seq[Double])

  private case class ImageMapRecord(navigation: Seq[Seq[Int]], areas: Seq[ImageMapArea])


  trait Options extends ScallopConf with DatabaseConfigurationOptions {

    val imageMapsDir = opt[String](required = true, noshort = true)
    val remappingFile = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  private def loadImageMap(id: String) = {
    val path = s"${options.imageMapsDir()}/$id.imagemap"
    println(s"Loading image map from $path")
    read[ImageMapRecord](Files.readAllLines(Paths.get(path)).asScala.mkString)
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

          val imageMapId = SQL("INSERT INTO image_maps VALUES(DEFAULT,{base_image_id})").on('base_image_id -> processedBaseImageId).executeInsert()

          val imageMapRecord = loadImageMap(r.id)

          val flatNav = imageMapRecord.navigation.flatten

          val imageMapObjectParams = imageMapRecord.areas.map {
            area =>
              val navIndex = flatNav.indexOf(area.id)
              val podgon = s"{${area.coords.mkString(",")}}"
              Seq[NamedParameter]('id -> area.id.toLong, 'image_map_id -> imageMapId, 'navigation_index -> navIndex, 'outline_coordinates -> podgon, 'overlay_image_id -> processedOverlayIds(area.id))
          }

          println(s"Create image map objects for ${r.id}")




          BatchSql("INSERT INTO image_map_objects VALUES({id},{image_map_id},{navigation_index},{outline_coordinates}::double precision[],{overlay_image_id})", imageMapObjectParams.head, imageMapObjectParams.tail: _*).execute()


          val guideObjectParams = r.overlayPaths.map {
            case (objectId, _) =>
              Seq[NamedParameter]('image_map_id -> imageMapId, 'guide_image_id -> r.id, 'object_id -> objectId)
          }

          println(s"Updating guide_image_objects image map and object ids")

          BatchSql("UPDATE guide_image_objects SET image_map_object_id=object_id, image_map_id={image_map_id} WHERE guide_image_id={guide_image_id} AND object_id={object_id}", guideObjectParams.head, guideObjectParams.tail: _*).execute()
      }
  }
}
