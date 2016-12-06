package uk.ac.ncl.openlab.intake24.foodsql.user

import scala.Left
import scala.Right
import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.GuideImageWeightRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound

import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.services.fooddb.user.UserGuideImage
import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.ImageMap
import uk.ac.ncl.openlab.intake24.services.fooddb.user.ImageMapObject
import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageObject

trait GuideImageUserImpl extends GuideImageService with FoodDataSqlService with SqlResourceLoader {
  private case class GuideResultRow(id: String, description: String, base_image_path: String, selection_image_path: String)
  private case class GuideObjectResultRow(object_id: Long, object_description: String, outline: Array[Double], object_weight: Double, overlay_image_path: String)

  private lazy val guideImageQuery = sqlFromResource("user/get_guide_image.sql")

  private lazy val guideImageObjectsQuery = sqlFromResource("user/get_guide_image_objects.sql")

  def getGuideImage(id: String): Either[LookupError, UserGuideImage] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val imageResult = SQL(guideImageQuery).on('id -> id).executeQuery().as(Macro.namedParser[GuideResultRow].singleOpt)

        imageResult match {
          case Some(row) => {
            val objectsResult = SQL(guideImageObjectsQuery).on('id -> id).executeQuery().as(Macro.namedParser[GuideObjectResultRow].*)

            val imageMap = ImageMap(row.base_image_path, objectsResult.map(_.object_id.toInt).toArray, objectsResult.map {
              objectRow => ImageMapObject(objectRow.object_id.toInt, objectRow.overlay_image_path, objectRow.outline)
            })

            val guideImageObjects = objectsResult.foldLeft(Map[Int, GuideImageObject]()) {
              case (acc, objectRow) => acc + (objectRow.object_id.toInt -> GuideImageObject(objectRow.object_description, objectRow.object_weight))
            }

            Right(UserGuideImage(row.description, imageMap, guideImageObjects))
          }
          case None => Left(RecordNotFound(new RuntimeException(s"Guide image $id not found")))
        }
      }
  }
}
