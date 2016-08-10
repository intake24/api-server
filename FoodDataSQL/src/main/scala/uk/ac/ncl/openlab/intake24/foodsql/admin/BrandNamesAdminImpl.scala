package uk.ac.ncl.openlab.intake24.foodsql.admin

import anorm.SQL
import anorm.SqlParser.str
import anorm.NamedParameter.symbol
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.BrandNamesAdminService
import org.slf4j.LoggerFactory
import anorm.NamedParameter
import anorm.BatchSql

trait BrandNamesAdminImpl extends BrandNamesAdminService with SqlDataService {
  private val logger = LoggerFactory.getLogger(classOf[BrandNamesAdminImpl])

  def brandNames(foodCode: String, locale: String): Either[DatabaseError, Seq[String]] = tryWithConnection {
    implicit conn =>
      // FIXME: check food code
      Right(SQL("""SELECT name FROM brands WHERE food_code = {food_code} AND locale_id = {locale_id} ORDER BY id""")
        .on('food_code -> foodCode, 'locale_id -> locale)
        .executeQuery()
        .as(str("name").*))
  }

  def deleteAllBrandNames(): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      logger.info("Deleting existing brand definitions")

      SQL("DELETE FROM brands").execute()

      Right(())
  }

  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      if (!brandNames.isEmpty) {
        conn.setAutoCommit(false)
        logger.info("Writing " + brandNames.size + " brands to database")
        val brandParams = brandNames.keySet.toSeq.flatMap(k => brandNames(k).map(name => Seq[NamedParameter]('food_code -> k, 'locale_id -> locale, 'name -> name)))

        BatchSql("""INSERT INTO brands VALUES(DEFAULT, {food_code}, {locale_id}, {name})""", brandParams).execute()
        conn.commit()

        Right(())
      } else {
        logger.warn("createBrandNames request with empty brand names map")
        Right(())
      }
  }
}
