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
import uk.ac.ncl.openlab.intake24.foodsql.SqlResourceLoader

trait AsServedImageUserImpl extends AsServedImageService with SqlDataService with SqlResourceLoader {

  protected case class AsServedResultRow(id: String, description: String, weight: Double, url: String)

  private lazy val getAsServedSetQuery = sqlFromResource("user/get_as_served_set.sql")

  def getAsServedSet(id: String): Either[LookupError, AsServedSet] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val validation = SQL("SELECT description FROM as_served_sets WHERE id={id}").on('id -> id).executeQuery().as(SqlParser.str(1).singleOpt)

        validation match {
          case Some(description) => {
            val result = SQL(getAsServedSetQuery).on('id -> id).executeQuery().as(Macro.namedParser[AsServedResultRow].*)
            val images = result.map(row => AsServedImage(row.url, row.weight))
            Right(AsServedSet(id, description, images))
          }
          case None => Left(RecordNotFound)
        }
      }
  }
}
