package uk.ac.ncl.openlab.intake24.foodsql.user

import uk.ac.ncl.openlab.intake24.services.fooddb.user.BrandNamesService
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import anorm._
import anorm.SqlParser.str

trait BrandNamesUserImpl extends BrandNamesService with SqlDataService {
  
  def brandNames(foodCode: String, locale: String): Either[DatabaseError, Seq[String]] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT name FROM brands WHERE food_code = {food_code} AND locale_id = {locale_id} ORDER BY id""")
        .on('food_code -> foodCode, 'locale_id -> locale)
        .executeQuery()
        .as(str("name").*))
  }
}
