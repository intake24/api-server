package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.GuideHeader
import anorm._
import uk.ac.ncl.openlab.intake24.foodsql.user.GuideImageUserImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.GuideImageAdminService
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.GuideImage
import com.google.inject.Inject
import javax.sql.DataSource
import com.google.inject.name.Named

class GuideImageAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends GuideImageAdminImpl

trait GuideImageAdminImpl extends GuideImageAdminService with GuideImageUserImpl {

  private val logger = LoggerFactory.getLogger(classOf[GuideImageAdminImpl])

  def listGuideImages(): Either[DatabaseError, Seq[GuideHeader]] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT id, description from guide_images ORDER BY description ASC""").executeQuery().as(Macro.namedParser[GuideHeader].*))
  }

  def deleteAllGuideImages(): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      logger.debug("Deleting existing guide image definitions")
      SQL("DELETE FROM guide_images").execute()
      Right(())
  }

  def createGuideImages(guideImages: Seq[GuideImage]): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      if (!guideImages.isEmpty) {
        conn.setAutoCommit(false)
        logger.debug("Writing " + guideImages.size + " guide images to database")

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
          logger.debug("Writing " + weightParams.size + " guide image weight records to database")
          batchSql("""INSERT INTO guide_image_weights VALUES (DEFAULT,{guide_image_id},{object_id},{description},{weight})""", weightParams).execute()
        } else
          logger.debug("Guide image file contains no object weight records")

        conn.commit()
        Right(())
      } else {
        logger.debug("createGuideImages request with empty guide image list")
        Right(())
      }
  }
}
