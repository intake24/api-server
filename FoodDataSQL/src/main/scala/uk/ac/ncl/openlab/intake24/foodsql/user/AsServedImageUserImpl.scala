package uk.ac.ncl.openlab.intake24.foodsql.user

import uk.ac.ncl.openlab.intake24.AsServedSet
import anorm._
import uk.ac.ncl.openlab.intake24.AsServedImage
import uk.ac.ncl.openlab.intake24.services.fooddb.user.AsServedImageService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import anorm.NamedParameter.symbol
import scala.Left
import scala.Right
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService

trait AsServedImageUserImpl extends AsServedImageService with SqlDataService {

  protected case class AsServedResultRow(id: String, description: String, weight: Double, url: String)

  def getAsServedSet(id: String): Either[LookupError, AsServedSet] = tryWithConnection {
    implicit conn =>
      val query =
        """|SELECT as_served_sets.id, description, weight, url
           |FROM as_served_sets JOIN as_served_images ON as_served_sets.id = as_served_set_id
           |WHERE as_served_sets.id = {id} ORDER BY as_served_images.weight""".stripMargin

      val result = SQL(query).on('id -> id).executeQuery().as(Macro.namedParser[AsServedResultRow].*)

      if (result.isEmpty)
        Left(RecordNotFound)
      else {
        val images = result.map(row => AsServedImage(row.url, row.weight))

        Right(AsServedSet(result.head.id, result.head.description, images))
      }
  }
}