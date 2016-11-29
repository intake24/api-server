package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.file.{Files, Paths}

import anorm.{AnormUtil, BatchSql, NamedParameter}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ProcessedImagePurpose
import uk.ac.ncl.openlab.intake24.sql.tools._
import upickle.default._

import scala.collection.JavaConverters._

object FoodV17_Guide_GenerateThumbs extends App with MigrationRunner with WarningMessage {

  private case class RemappedGuideImage(id: String, url: String, sourcePath: String, mainImagePath: String, overlayPaths: Seq[(Long, String)])

  private case class RemappedGuideImageWithThumb()

  trait Options extends ScallopConf with DatabaseConfigurationOptions {

    val remappingFile = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  runMigration(17, 18, options) {
    implicit conn =>

      val remapped = read[Seq[RemappedGuideImage]](Files.readAllLines(Paths.get(options.remappingFile())).asScala.mkString)

      val sourceParams = remapped.map {
        m =>
          Seq[NamedParameter]('path -> m.sourcePath, 'uploader -> "admin")
      }

      println("Creating source image records")

      val keys = AnormUtil.batchKeys(BatchSql("INSERT INTO source_images VALUES (DEFAULT,{path},{uploader},DEFAULT)", sourceParams.head, sourceParams.tail: _*))

      val processedMainParams = remapped.zip(keys).map {
        case (r, sourceKey) =>
          Seq[NamedParameter]('path -> r.mainImagePath, 'source_id -> sourceKey, 'purpose -> ProcessedImagePurpose.toId(ProcessedImagePurpose.GuideMainImage))
      }

      println("Creating processed image records for base images")

      val processedMainKeys = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedMainParams.head, processedMainParams.tail: _*))

      val guideImageParams = remapped.zip(processedMainKeys).map {
        case (r, imageId) =>
          Seq[NamedParameter]('id -> r.id, 'url -> r.url, 'image_id -> imageId)
      }

      println("Updating guide_images with base image ids")

      BatchSql("UPDATE guide_images SET image_id={image_id} WHERE id={id} AND base_image_url={url}", guideImageParams.head, guideImageParams.tail: _*).execute()

      remapped.zip(keys).foreach {
        case (r, sourceKey) =>

          val processedOverlayParams = r.overlayPaths.map {
            case (objectId, path) =>
              Seq[NamedParameter]('path -> path, 'source_id -> sourceKey, 'purpose -> ProcessedImagePurpose.toId(ProcessedImagePurpose.GuideOverlay))
          }

          println(s"Creating processed image records for overlays for ${r.id}")

          val processedOverlayKeys = AnormUtil.batchKeys(BatchSql("INSERT INTO processed_images VALUES(DEFAULT,{path},{source_id},{purpose},DEFAULT)", processedOverlayParams.head, processedOverlayParams.tail: _*))


          val objParams = r.overlayPaths.map(_._1).zip(processedOverlayKeys).map {
            case (objectId, processedImageId) =>
              Seq[NamedParameter]('guide_image_id -> r.id, 'object_id -> objectId, 'overlay_image_id -> processedImageId)
          }

          println(s"Updating guide_image_objects with processed overlay image ids for ${r.id}")


          BatchSql("UPDATE guide_image_objects SET overlay_image_id={overlay_image_id} WHERE guide_image_id={guide_image_id} AND object_id={object_id}", objParams.head, objParams.tail: _*).execute()
      }
  }
}
