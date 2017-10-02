package uk.ac.ncl.openlab.intake24.foodsql.admin

import javax.inject.Named
import javax.sql.DataSource

import anorm.{Macro, NamedParameter, SQL}
import com.google.inject.{Inject, Singleton}
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{GuideImageAdminService, NewGuideImageRecord}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import uk.ac.ncl.openlab.intake24.{GuideHeader, GuideImageFull, GuideImageMapObject}

@Singleton
class GuideImageAdminImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource, guideImageService: GuideImageService, imageStorage: ImageStorageService) extends GuideImageAdminService with SqlDataService {

  private case class GuidedImageObjectRow(id: String,
                                          description: String,
                                          image_map_id: String,
                                          path: String,
                                          image_map_object_id: Int,
                                          weight: Double,
                                          image_map_object_description: String,
                                          outline_coordinates: Array[Double])

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
           |  imo.description AS image_map_object_description,
           |  imo.outline_coordinates
           |FROM guide_images AS gi
           |  JOIN image_maps ON gi.image_map_id = image_maps.id
           |  JOIN processed_images AS pi ON image_maps.base_image_id = pi.id
           |  JOIN image_map_objects AS imo ON imo.image_map_id = image_maps.id
           |  JOIN guide_image_objects AS gio ON imo.id = gio.image_map_object_id
           |                                  AND gio.guide_image_id = gi.id
           |WHERE gi.id = {id}""".stripMargin

      withTransaction {
        val imageResult = SQL(q).on('id -> id).executeQuery().as(Macro.namedParser[GuidedImageObjectRow].*)

        imageResult match {
          case Nil => Left(RecordNotFound(new RuntimeException(s"Guide image $id not found")))
          case l =>
            val gi = l.head
            val imageMapObjects = l.map { io => GuideImageMapObject(io.image_map_object_id, io.weight, io.image_map_object_description, io.outline_coordinates) }
            Right(GuideImageFull(gi.id, gi.description, imageStorage.getUrl(gi.path), imageMapObjects))
        }
      }
  }

  def getGuideImage(id: String) = guideImageService.getGuideImage(id)
}
