package uk.ac.ncl.openlab.intake24.foodsql.user

import scala.Left
import scala.Right

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.user.AsServedImageService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.UserAsServedImage
import uk.ac.ncl.openlab.intake24.AsServedImageV1
import uk.ac.ncl.openlab.intake24.services.fooddb.user.UserAsServedSet

trait AsServedImageUserImpl extends AsServedImageService with FoodDataSqlService with SqlResourceLoader {

  private case class AsServedImageRow(image_path: String, thumbnail_path: String, weight: Double)

  private case class AsServedSetRow(selection_image_path: String)

  private lazy val setQuery = sqlFromResource("user/get_as_served_set.sql")

  private lazy val imagesQuery = sqlFromResource("user/get_as_served_images.sql")

  def getAsServedSet(id: String): Either[LookupError, UserAsServedSet] = tryWithConnection {
    implicit conn =>
      withTransaction {
        SQL(setQuery).on('id -> id).executeQuery().as(Macro.namedParser[AsServedSetRow].singleOpt) match {
          case Some(set) => {
            val result = SQL(imagesQuery).on('as_served_set_id -> id).executeQuery().as(Macro.namedParser[AsServedImageRow].*)
            val images = result.map(row => UserAsServedImage(row.image_path, row.thumbnail_path, row.weight))
            Right(UserAsServedSet(set.selection_image_path, images))
          }
          case None => Left(RecordNotFound(new RuntimeException(id)))
        }
      }
  }
}
