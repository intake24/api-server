package uk.ac.ncl.openlab.intake24.foodsql.user

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, sqlToSimple}
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{GuideImageService, UserGuideImage}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

trait GuideImageUserImpl extends GuideImageService with SqlDataService with SqlResourceLoader {

  private case class GuideResultRow(description: String, image_map_id: String, object_id: Array[Long], object_weight: Array[Double])

  private lazy val guideImageQuery = sqlFromResource("user/get_guide_image.sql")

  def getGuideImage(id: String): Either[LookupError, UserGuideImage] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val imageResult = SQL(guideImageQuery).on('id -> id).executeQuery().as(Macro.namedParser[GuideResultRow].singleOpt)

        imageResult match {
          case Some(row) => {
            Right(UserGuideImage(row.description, row.image_map_id, row.object_id.map(_.toInt).zip(row.object_weight).toMap))
          }
          case None => Left(RecordNotFound(new RuntimeException(s"Guide image $id not found")))
        }
      }
  }
}
