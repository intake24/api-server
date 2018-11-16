package uk.ac.ncl.openlab.intake24.foodsql.admin

import anorm.Macro.ColumnNaming
import anorm.{Macro, NamedParameter, SQL, SqlParser}
import com.google.inject.{Inject, Singleton}
import javax.inject.Named
import javax.sql.DataSource
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.data.GuideHeader
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{ImageAdminService, ImageStorageService}
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
          |SELECT id, description, image_map_id FROM guide_images ORDER BY id ASC;
        """.stripMargin).executeQuery().as(Macro.namedParser[GuideHeader](ColumnNaming.SnakeCase).*)
      Right(headers)
  }

  def deleteAllGuideImages(): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      logger.debug("Deleting existing guide image definitions")
      SQL("DELETE FROM guide_images").execute()
      Right(())
  }

  override def deleteGuideImage(id: String): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      logger.debug(s"Trying to delete guide image $id")

      withTransaction {
        val count = SQL("select count(*) from foods_portion_size_method_params where name='guide-image-id' and value={id}")
          .on('id -> id)
          .as(SqlParser.int(1).single)

        if (count > 0)
          Left(StillReferenced(new RuntimeException("Guide image is still referenced. Remove all portion size estimation methods using this guide image first.")))
        else {

          val imageMapId = SQL("select image_map_id from guide_images where id={id}")
            .on('id -> id)
            .as(SqlParser.str(1).single)

          SQL("delete from guide_images where id={id}")
            .on('id -> id)
            .executeUpdate()

          val usageCount = SQL("select count(*) from guide_images where image_map_id={imageMapId}")
            .on('imageMapId -> imageMapId)
            .as(SqlParser.long(1).single)

          if (usageCount > 0)
            logger.warn(s"Keeping image map $imageMapId because it is used by other guide images. This is OK, but unexpected.")
          else {
            logger.debug(s"Deleting unused image map $imageMapId")
            SQL("delete from image_maps where id={imageMapId}")
              .on('imageMapId -> imageMapId)
              .execute()
          }

          Right(())
        }
      }
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
                    Seq[NamedParameter]('guide_image_id -> image.id, 'object_id -> objectId, 'weight -> weight)
                }
            }

            if (!weightParams.isEmpty) {
              logger.debug("Writing " + weightParams.size + " guide image weight records to database")
              batchSql("""INSERT INTO guide_image_objects(id, guide_image_id, image_map_object_id, weight) VALUES (DEFAULT,{guide_image_id},{object_id},{weight})""", weightParams).execute()
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


  def getGuideImage(id: String) = guideImageService.getGuideImage(id)

  override def getImageMapId(id: String): Either[LookupError, String] = tryWithConnection {
    implicit conn =>
      val imageMapId = SQL("SELECT image_map_id FROM guide_images WHERE id = {id};")
        .on('id -> id).executeQuery().as(SqlParser.str(1).single)
      Right(imageMapId)
  }

  override def updateGuideImageMeta(id: String, meta: GuideImageMeta): Either[UpdateError, GuideImageMeta] = tryWithConnection {
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

  override def updateGuideImageObjects(id: String, objects: Seq[GuideImageMapObject]): Either[UpdateError, Unit] =
    tryWithConnection {
      implicit conn =>
        withTransaction {
          SQL("delete from guide_image_objects where guide_image_id={id}")
            .on('id -> id)
            .execute()

          val objectParams = objects.map {
            obj =>
              Seq[NamedParameter]('guide_image_id -> id, 'object_id -> obj.objectId, 'weight -> obj.weight)
          }

          if (objectParams.nonEmpty)
            batchSql("insert into guide_image_objects(guide_image_id, image_map_object_id, weight) VALUES ({guide_image_id},{object_id},{weight})", objectParams).execute()

          Right(())
        }
    }
}
