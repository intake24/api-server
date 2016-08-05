package uk.ac.ncl.openlab.intake24.foodsql.admin

import anorm.SQL
import anorm.SqlParser.str
import anorm.NamedParameter.symbol
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.BrandNamesAdminService

trait BrandNamesAdminServiceImpl extends BrandNamesAdminService with SqlDataService {
  def brandNames(foodCode: String, locale: String): Either[DatabaseError, Seq[String]] = tryWithConnection {
    implicit conn =>
      
      
      
      Right(SQL("""SELECT name FROM brands WHERE food_code = {food_code} AND locale_id = {locale_id} ORDER BY id""")
        .on('food_code -> foodCode, 'locale_id -> locale)
        .executeQuery()
        .as(str("name").*))
  }
}
