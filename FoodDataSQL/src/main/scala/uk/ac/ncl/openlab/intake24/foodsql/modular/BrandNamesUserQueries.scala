package uk.ac.ncl.openlab.intake24.foodsql.modular

import scala.Right

import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.SqlParser
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService

trait BrandNamesUserQueries extends FoodDataSqlService with FirstRowValidation with SqlResourceLoader {

  private lazy val getBrandNamesQuery = sqlFromResource("user/get_brand_names_frv.sql")

  protected def getBrandNamesQuery(foodCode: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[String]] = {
    val result = SQL(getBrandNamesQuery).on('food_code -> foodCode, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndFoodValidation(foodCode, result, SqlParser.str("name").+)(Seq(FirstRowValidationClause("name", () => Right(List()))))
  }
}
