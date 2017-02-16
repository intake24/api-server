package uk.ac.ncl.openlab.intake24.foodsql.modular

import anorm.NamedParameter.symbol
import anorm.{SQL, SqlParser, sqlToSimple}
import uk.ac.ncl.openlab.intake24.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.foodsql.{FirstRowValidation, FirstRowValidationClause}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

trait BrandNamesUserQueries extends SqlDataService with FirstRowValidation with SqlResourceLoader {

  private lazy val getBrandNamesQuery = sqlFromResource("user/get_brand_names_frv.sql")

  protected def getBrandNamesQuery(foodCode: String, locale: String)(implicit conn: java.sql.Connection): Either[LocalLookupError, Seq[String]] = {
    val result = SQL(getBrandNamesQuery).on('food_code -> foodCode, 'locale_id -> locale).executeQuery()

    parseWithLocaleAndFoodValidation(foodCode, result, SqlParser.str("name").+)(Seq(FirstRowValidationClause("name", () => Right(List()))))
  }
}
