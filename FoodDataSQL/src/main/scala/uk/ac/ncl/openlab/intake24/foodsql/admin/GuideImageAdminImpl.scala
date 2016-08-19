package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.GuideHeader
import anorm._
import uk.ac.ncl.openlab.intake24.foodsql.user.GuideImageUserImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.GuideImageAdminService
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.GuideImage

trait GuideImageAdminImpl extends GuideImageAdminService with GuideImageUserImpl {

  private val logger = LoggerFactory.getLogger(classOf[GuideImageAdminImpl])

  def allGuideImages(): Either[DatabaseError, Seq[GuideHeader]] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT id, description from guide_images ORDER BY description ASC""").executeQuery().as(Macro.namedParser[GuideHeader].*))
  }

  def deleteAllGuideImages(): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      logger.info("Deleting existing guide image definitions")
      SQL("DELETE FROM guide_images").execute()
      Right(())
  }

  def createGuideImages(guideImages: Seq[GuideImage]): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      if (!guideImages.isEmpty) {
        conn.setAutoCommit(false)
        logger.info("Writing " + guideImages.size + " guide images to database")

        val guideImageParams = guideImages.map {
          image => Seq[NamedParameter]('id -> image.id, 'description -> image.description, 'base_image_url -> (image.id + ".jpg"))
        }.toSeq

        batchSql("""INSERT INTO guide_images VALUES ({id},{description},{base_image_url})""", guideImageParams).execute()

        val weightParams = guideImages.flatMap {
          case image =>
            image.weights.map {
              weight =>
                Seq[NamedParameter]('guide_image_id -> image.id, 'object_id -> weight.objectId, 'description -> weight.description, 'weight -> weight.weight)
            }
        }.toSeq

        if (!weightParams.isEmpty) {
          logger.info("Writing " + weightParams.size + " guide image weight records to database")
          batchSql("""INSERT INTO guide_image_weights VALUES (DEFAULT,{guide_image_id},{object_id},{description},{weight})""", weightParams).execute()
        } else
          logger.warn("Guide image file contains no object weight records")

        conn.commit()
        Right(())
      } else {
        logger.warn("createGuideImages request with empty guide image list")
        Right(())
      }
  }
}
