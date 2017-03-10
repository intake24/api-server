package uk.ac.ncl.openlab.intake24.foodsql.user

import javax.inject.Inject
import javax.sql.DataSource

import anorm.NamedParameter.symbol
import anorm.{Macro, SQL, sqlToSimple}
import com.google.inject.Singleton
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{AsServedSetsService, UserAsServedImage, UserAsServedSet}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

@Singleton
class AsServedSetsServiceImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends AsServedSetsService with SqlDataService with SqlResourceLoader {

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
