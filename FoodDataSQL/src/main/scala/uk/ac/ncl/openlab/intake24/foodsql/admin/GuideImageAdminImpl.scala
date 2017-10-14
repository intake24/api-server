package uk.ac.ncl.openlab.intake24.foodsql.admin

import java.awt.Shape
import java.sql.Connection
import javax.inject.Named
import javax.sql.DataSource

import anorm.{Macro, NamedParameter, SQL, SqlParser}
import com.google.inject.{Inject, Singleton}
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{AWTImageMap, ImageAdminService, ImageStorageService, ShapeFactory}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

@Singleton
class GuideImageAdminImpl @Inject()(@Named("intake24_foods")
                                    val dataSource: DataSource,
                                    guideImageService: GuideImageService,
                                    imageStorage: ImageStorageService,
                                    imageAdminService: ImageAdminService,
                                    imageMapsAdminService: ImageMapsAdminService) extends GuideImageAdminService with SqlDataService {

  private case class GuidedImageMapRow(id: String,
                                       description: String,
                                       image_map_id: String,
                                       path: String,
                                       image_map_object_id: Option[Long],
                                       weight: Option[Double],
                                       navigation_index: Option[Int],
                                       image_map_object_description: Option[String],
                                       outline_coordinates: Option[Array[Double]])

  private case class GuideImageMapObjectRow(image_map_id: String,
                                            image_map_object_id: Long,
                                            description: String, navigation_index: Int,
                                            outline_coordinates: Array[Double], weight: Double)

  private case class GuideImageMetaRow(id: String, description: String) {
    def toGuideImageMeta = GuideImageMeta(id, description)
  }

  private val logger = LoggerFactory.getLogger(classOf[GuideImageAdminImpl])

  def listGuideImages(): Either[UnexpectedDatabaseError, Seq[GuideHeader]] = tryWithConnection {
    implicit conn =>
      val headers = SQL(
        """
          |SELECT
          |  guide_images.id,
          |  guide_images.description,
          |  processed_images.path
          |FROM guide_images
          |  JOIN image_maps ON guide_images.image_map_id = image_maps.id
          |  JOIN processed_images ON image_maps.base_image_id = processed_images.id
          |ORDER BY guide_images.id ASC;
        """.stripMargin).executeQuery().as(Macro.namedParser[GuideHeader].*)
      Right(headers)
  }

  def deleteAllGuideImages(): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      logger.debug("Deleting existing guide image definitions")
      SQL("DELETE FROM guide_images").execute()
      Right(())
  }

  def updateGuideSelectionImage(id: String, selectionImageId: Long) = tryWithConnection {
    implicit conn =>
      tryWithConstraintCheck[DependentUpdateError, Unit]("guide_selection_image_id_fk", e => ParentRecordNotFound(e)) {
        val updatedCount = SQL("UPDATE guide_images SET selection_image_id={image_id} WHERE id={id}")
          .on('id -> id, 'image_id -> selectionImageId)
          .executeUpdate()

        if (updatedCount == 1)
          Right(())
        else
          Left(RecordNotFound(new RuntimeException("Guide image not found")))
      }
  }

  def createGuideImages(guideImages: Seq[NewGuideImageRecord]): Either[DependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      if (!guideImages.isEmpty) {
        withTransaction {

          val errors = Map[String, PSQLException => DependentCreateError](
            "guide_image_object_fk" -> (e => ParentRecordNotFound(e)),
            "guide_image_weights_guide_image_id_fk" -> (e => ParentRecordNotFound(e)))

          tryWithConstraintsCheck[DependentCreateError, Unit](errors) {
            logger.debug("Writing " + guideImages.size + " guide images to database")

            val guideImageParams = guideImages.map {
              image => Seq[NamedParameter]('id -> image.id, 'description -> image.description, 'image_map_id -> image.imageMapId, 'selection_image_id -> image.selectionImageId)
            }

            batchSql("""INSERT INTO guide_images VALUES ({id},{description},{image_map_id},{selection_image_id})""", guideImageParams).execute()

            val weightParams = guideImages.flatMap {
              case image =>
                image.objectWeights.map {
                  case (objectId, weight) =>
                    Seq[NamedParameter]('guide_image_id -> image.id, 'image_map_id -> image.imageMapId, 'object_id -> objectId, 'weight -> weight)
                }
            }

            if (!weightParams.isEmpty) {
              logger.debug("Writing " + weightParams.size + " guide image weight records to database")
              batchSql("""INSERT INTO guide_image_objects(id, guide_image_id, image_map_id, image_map_object_id, weight) VALUES (DEFAULT,{guide_image_id},{image_map_id},{object_id},{weight})""", weightParams).execute()
            } else
              logger.debug("Guide image file contains no object weight records")

            Right(())
          }
        }
      }
      else {
        logger.debug("createGuideImages request with empty guide image list")
        Right(())
      }
  }

  def getFullGuideImage(id: String) = tryWithConnection {
    implicit conn =>

      val q =
        """|SELECT
           |  gi.id,
           |  gi.description,
           |  gi.image_map_id,
           |  pi.path,
           |  gio.image_map_object_id,
           |  gio.weight,
           |  imo.navigation_index,
           |  imo.description AS image_map_object_description,
           |  imo.outline_coordinates
           |FROM guide_images AS gi
           |  LEFT JOIN image_maps ON gi.image_map_id = image_maps.id
           |  LEFT JOIN processed_images AS pi ON image_maps.base_image_id = pi.id
           |  LEFT JOIN image_map_objects AS imo ON imo.image_map_id = image_maps.id
           |  LEFT JOIN guide_image_objects AS gio ON imo.id = gio.image_map_object_id
           |                                  AND gio.guide_image_id = gi.id
           |WHERE gi.id = {id}""".stripMargin

      withTransaction {
        val imageResult = SQL(q).on('id -> id).executeQuery().as(Macro.namedParser[GuidedImageMapRow].*)

        imageResult match {
          case Nil => Left(RecordNotFound(new RuntimeException(s"Guide image $id not found")))
          case l =>
            val gi = l.head
            val imageMapObjects = l.flatMap { io =>
              for (
                weight <- io.weight;
                img_map_object_description <- io.image_map_object_description;
                nav_index <- io.navigation_index;
                outline_coordinates <- io.outline_coordinates;
                obj = GuideImageMapObject(weight, img_map_object_description, nav_index, outline_coordinates)
              ) yield obj
            }
            val imageMeta = GuideImageMeta(gi.id, gi.description)
            Right(GuideImageFull(imageMeta, gi.image_map_id, imageStorage.getUrl(gi.path), imageMapObjects))
        }
      }
  }

  def getGuideImage(id: String) = guideImageService.getGuideImage(id)

  override def getImageMapId(id: String): Either[LookupError, String] = tryWithConnection {
    implicit conn =>
      val imageMapId = SQL("SELECT image_map_id FROM guide_images WHERE id = {id};")
        .on('id -> id).executeQuery().as(SqlParser.str(1).single)
      Right(imageMapId)
  }

  override def patchGuideImageMeta(id: String, meta: GuideImageMeta): Either[UpdateError, GuideImageMeta] = tryWithConnection {
    implicit conn =>
      val q =
        """
          |UPDATE guide_images
          |SET id = {new_id}, description = {new_description}
          |WHERE id = {id}
          |RETURNING id, description;
        """.stripMargin
      SQL(q).on('new_id -> meta.id, 'new_description -> meta.description, 'id -> id)
        .executeQuery().as(Macro.namedParser[GuideImageMetaRow].singleOpt) match {
        case None => Left(RecordNotFound(new RuntimeException(s"No guide image with id: $id was found")))
        case Some(row) => Right(row.toGuideImageMeta)
      }
  }

  override def patchGuideImageObjects(id: String, aspectRatio: Double, objects: Seq[GuideImageMapObject]): Either[UpdateError, Seq[GuideImageMapObject]] =
    tryWithConnection { implicit conn =>

      val errorMessage = "Failed to update GuideImageObjects"
      val cleanQ =
        """
          |DELETE FROM guide_image_objects WHERE guide_image_id = {id};
          |DELETE FROM image_map_objects
          |WHERE image_map_id = (SELECT image_map_id FROM guide_images WHERE id = {id});
        """.stripMargin

      val procObj = objects.zipWithIndex
        .map(i => (i._1, ShapeFactory.getShapeFromFlatCoordinates(i._1.outlineCoordinates), i._2))
        .filter(_._2.isDefined)

      for (
        imageMapId <- getImageMapId(id);
        sourceId <- imageMapsAdminService.getImageMapBaseImageSourceId(imageMapId);
        overlayDescriptors <- generateOverlays(imageMapId, sourceId, procObj.map(_._1.navigationIndex),
          procObj.map(i => i._3 -> i._2.get).toMap, aspectRatio);
        res <- withTransaction {
          SQL(cleanQ).on('id -> id).execute()
          val objects = procObj.map(_._1).zip(overlayDescriptors).map { obj =>
            createGuideImageObject(id, imageMapId, obj._2._1.toLong, obj._1, obj._2._2.id)
          }
          if (objects.exists(_.isLeft)) {
            Left(UnexpectedDatabaseError(new RuntimeException(errorMessage)))
          } else {
            Right(objects.map(_.right.get))
          }
        }
      ) yield res

    }

  override def deleteGuideImageObject(imageMapId: String, imageMapObjectId: Long): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      val delQ =
        """
          |DELETE FROM guide_image_objects WHERE image_map_object_id = {image_map_object_id} AND image_map_id = {image_map_id};
          |DELETE FROM image_map_objects WHERE id={image_map_object_id} AND image_map_id={image_map_id};
        """.stripMargin
      SQL(delQ).on('image_map_object_id -> imageMapObjectId, 'image_map_id -> imageMapId).execute()
      Right(())
  }

  private def generateOverlays(imageMapId: String, sourceId: Long, navIndexes: Seq[Int],
                               shapeMap: Map[Int, Shape], aspectRatio: Double) = {
    val imageMap = AWTImageMap(navIndexes, shapeMap, aspectRatio)
    imageAdminService.generateImageMapOverlays(imageMapId, sourceId, imageMap).left.map { e =>
      UnexpectedDatabaseError(e.exception)
    }
  }

  private def createGuideImageObject(guideImageId: String,
                                     imageMapId: String,
                                     imageMapObjectId: Long,
                                     obj: GuideImageMapObject,
                                     overlayImageId: Long)(implicit conn: Connection): Either[CreateError, GuideImageMapObject] = {

    val insertQ =
      """
        |WITH imo AS (
        |  INSERT INTO image_map_objects (image_map_id, id, description, navigation_index, outline_coordinates, overlay_image_id)
        |  VALUES ({image_map_id}, {id}, {description}, {navigation_index}, {outline_coordinates}::DOUBLE PRECISION[], {overlay_image_id})
        |  RETURNING image_map_id, id, description, navigation_index, outline_coordinates
        |), gio AS (
        |  INSERT INTO guide_image_objects (guide_image_id, weight, image_map_id, image_map_object_id)
        |  SELECT {guide_image_id}, {weight}, imo.image_map_id, imo.id FROM imo
        |  RETURNING image_map_id, weight
        |) SELECT
        |    imo.image_map_id,
        |    imo.id AS image_map_object_id,
        |    imo.description,
        |    imo.navigation_index,
        |    imo.outline_coordinates,
        |    gio.weight
        |FROM imo JOIN gio ON gio.image_map_id=imo.image_map_id;
      """.stripMargin
    val outline = s"{${obj.outlineCoordinates.toArray.mkString(",")}}"
    val row = SQL(insertQ).on(
      'image_map_id -> guideImageId,
      'id -> imageMapObjectId,
      'description -> obj.description,
      'navigation_index -> obj.navigationIndex,
      'outline_coordinates -> outline,
      'overlay_image_id -> overlayImageId,
      'guide_image_id -> guideImageId,
      'weight -> obj.weight
    ).executeQuery().as(Macro.namedParser[GuideImageMapObjectRow].single)
    Right(GuideImageMapObject(row.weight, row.description, row.navigation_index, row.outline_coordinates))
  }

}
