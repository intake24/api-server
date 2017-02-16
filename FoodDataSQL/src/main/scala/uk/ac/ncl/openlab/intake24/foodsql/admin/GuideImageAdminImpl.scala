package uk.ac.ncl.openlab.intake24.foodsql.admin

import javax.sql.DataSource

import anorm._
import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.foodsql.user.GuideImageUserImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{GuideImageAdminService, NewGuideImageRecord}
import uk.ac.ncl.openlab.intake24.errors._

@Singleton
class GuideImageAdminStandaloneImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends GuideImageAdminImpl

trait GuideImageAdminImpl extends GuideImageAdminService with GuideImageUserImpl {

  private val logger = LoggerFactory.getLogger(classOf[GuideImageAdminImpl])

  def listGuideImages(): Either[UnexpectedDatabaseError, Seq[GuideHeader]] = tryWithConnection {
    implicit conn =>
      val headers = SQL("""SELECT id, description from guide_images ORDER BY description ASC""").executeQuery().as(Macro.namedParser[GuideHeader].*)
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
}
