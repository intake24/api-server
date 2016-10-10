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

trait AsServedImageUserImpl extends AsServedImageService with FoodDataSqlService with SqlResourceLoader {

  protected case class AsServedResultRow(id: String, description: String, weight: Double, url: String)

  private lazy val getAsServedSetQuery = sqlFromResource("user/get_as_served_v1.sql")

  def getAsServedSet(id: String): Either[LookupError, Seq[AsServedImageV1]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val validation = SQL("SELECT 1 FROM as_served_sets WHERE id={id}").on('id -> id).executeQuery().as(SqlParser.long(1).singleOpt)

        validation match {
          case Some(description) => {
            val result = SQL(getAsServedSetQuery).on('id -> id).executeQuery().as(Macro.namedParser[AsServedResultRow].*)
            val images = result.map(row => AsServedImageV1(row.url, row.weight))
            Right(images)
          }
          case None => Left(RecordNotFound(new RuntimeException(id)))
        }
      }
  }
}
