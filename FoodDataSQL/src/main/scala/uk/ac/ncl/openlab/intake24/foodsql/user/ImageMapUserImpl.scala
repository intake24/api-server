package uk.ac.ncl.openlab.intake24.foodsql.user

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, sqlToSimple}
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{ImageMapService, UserImageMap, UserImageMapObject}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

trait ImageMapUserImpl extends ImageMapService with SqlDataService with SqlResourceLoader {

  private case class ResultRow(id: String, base_image_path: String, object_id: Long, description: String, outline: Array[Double], overlay_image_path: String)

  private lazy val imageMapObjectsQuery = sqlFromResource("user/get_image_map_objects.sql")

  def getImageMaps(ids: Seq[String]): Either[LookupError, Seq[UserImageMap]] = tryWithConnection {
    implicit conn =>
      val result = SQL(imageMapObjectsQuery).on('ids -> ids).as(Macro.namedParser[ResultRow].*).groupBy(_.id).map {
        case (id, rows) =>
          val objects = rows.map {
            row => UserImageMapObject(row.object_id.toInt, row.description, row.overlay_image_path, row.outline)
          }
          id -> UserImageMap(rows.head.base_image_path, objects)
      }

      val missing = ids.filterNot(result.contains(_))

      if (missing.nonEmpty)
        Left(RecordNotFound(new RuntimeException(s"Missing image maps: ${missing.mkString(", ")}")))
      else
        Right(ids.map(result(_)))
  }

  def getImageMap(id: String): Either[LookupError, UserImageMap] = getImageMaps(Seq(id)).right.map {
    _.head
  }
}
