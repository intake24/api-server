package uk.ac.ncl.openlab.intake24.foodsql.user

import javax.inject.Inject
import javax.sql.DataSource
import anorm.NamedParameter.symbol
import anorm.{~, SQL, SqlParser, sqlToSimple}
import com.google.inject.Singleton
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{GuideImageService, UserGuideImage}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

@Singleton
class GuideImageServiceImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends GuideImageService with SqlDataService with SqlResourceLoader {

  def getGuideImage(id: String): Either[LookupError, UserGuideImage] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL("select description, image_map_id from guide_images where id={id}")
          .on('id -> id)
          .executeQuery()
          .as((SqlParser.str(1) ~ SqlParser.str(2)).singleOpt) match {
          case Some(description ~ imageMapId) =>

            val objects = SQL("select image_map_object_id, weight from guide_image_objects where guide_image_id={id}")
              .on('id -> id)
              .executeQuery()
              .as((SqlParser.int(1) ~ SqlParser.double(2)).*).foldLeft(Map[Int, Double]()) {
              case (result, objectId ~ weight) => result + (objectId -> weight)
            }

            Right(UserGuideImage(description, imageMapId, objects))

          case None =>
            Left(RecordNotFound(new RuntimeException(s"Guide image $id not found")))
        }
      }
  }
}
