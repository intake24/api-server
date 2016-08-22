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

import uk.ac.ncl.openlab.intake24.foodsql.user.BrandNamesUserImpl
import java.sql.Connection
import uk.ac.ncl.openlab.intake24.foodsql.SimpleValidation
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale

trait BrandNamesAdminImpl extends BrandNamesAdminService with SqlDataService with BrandNamesUserImpl with SimpleValidation {
  private val logger = LoggerFactory.getLogger(classOf[BrandNamesAdminImpl])

  def deleteAllBrandNames(locale: String): Either[LocaleError, Unit] = tryWithConnection {
    implicit conn =>
      logger.debug(s"Deleting all brand definitions for locale $locale")

      withTransaction {
        validateLocale(locale).right.flatMap {
          _ =>
            {
              SQL("DELETE FROM brands WHERE locale_id={locale_id}").on('locale_id -> locale).execute()
              Right(())
            }
        }
      }
  }

  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[LocalDependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      val brandParams = brandNames.keySet.toSeq.flatMap(k => brandNames(k).map(name => Seq[NamedParameter]('food_code -> k, 'locale_id -> locale, 'name -> name)))
      
      if (!brandParams.isEmpty) {
        conn.setAutoCommit(false)
        logger.debug("Writing " + brandNames.size + " brands to database")

        val constraintErrors = Map(
          "brands_food_locale_fk" -> UndefinedLocale,
          "brands_food_code_fk" -> ParentRecordNotFound)

        tryWithConstraintsCheck(constraintErrors) {
          batchSql("""INSERT INTO brands VALUES(DEFAULT, {food_code}, {locale_id}, {name})""", brandParams).execute()
          conn.commit()
          Right(())
        }
      } else {
        logger.debug("createBrandNames request with empty brand names map")
        Right(())
      }
  }
}
