package uk.ac.ncl.openlab.intake24.foodsql.modular

import java.sql.Connection

import anorm.NamedParameter.symbol
import anorm.{SQL, SqlParser, sqlToSimple}
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

trait FoodQueries extends SqlDataService {

  def foodCodeExistsQuery(code: String)(implicit connection: Connection): Either[UnexpectedDatabaseError, Boolean] =
    Right(SQL("""SELECT code FROM foods WHERE code={food_code}""").on('food_code -> code).executeQuery().as(SqlParser.str("code").*).nonEmpty)

  def validateFoodCodeQuery(code: String)(implicit connection: Connection): Either[LookupError, Unit] =
    foodCodeExistsQuery(code).flatMap {
      if (_) Right(()) else Left(RecordNotFound(new RuntimeException(s"Food code $code is undefined")))
    }

}
