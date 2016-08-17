package uk.ac.ncl.openlab.intake24.foodsql.user

import uk.ac.ncl.openlab.intake24.services.fooddb.user.BrandNamesService
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import anorm._
import anorm.SqlParser.str
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidationClause
import uk.ac.ncl.openlab.intake24.foodsql.FirstRowValidation

trait BrandNamesUserImpl extends BrandNamesService with SqlDataService with FirstRowValidation {

  private case class Validation(foodCodeValid: Boolean, localeIdValid: Boolean, hasRows: Boolean)

  def brandNames(foodCode: String, locale: String): Either[LocalLookupError, Seq[String]] = tryWithConnection {
    implicit conn =>

      // see http://stackoverflow.com/a/38793141/622196 for explanation

      val query = """|WITH v AS ( 
                     |  SELECT (SELECT code FROM foods WHERE code={food_code}) AS food_code, 
                     |  SELECT (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
                     |)
                     |SELECT v.food_code, v.locale_id, brands.name FROM 
                     |v LEFT JOIN brands ON v.food_code = brands.food_code AND v.locale_id = brands.locale_id""".stripMargin

      val result = SQL(query).on('food_code -> foodCode, 'locale_id -> locale).executeQuery()

      parseWithLocaleAndFoodValidation(result, SqlParser.str("name").+)(Seq(FirstRowValidationClause("name", Right(List()))))      
  }
}
