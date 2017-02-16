package uk.ac.ncl.openlab.intake24.foodsql.modular

import anorm.NamedParameter.symbol
import anorm.{NamedParameter, SQL, sqlToSimple}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{LocaleOrParentError, ParentRecordNotFound, UndefinedLocale}
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

trait BrandNamesAdminQueries extends SqlDataService {
  private val logger = LoggerFactory.getLogger(classOf[BrandNamesAdminQueries])

  protected def deleteBrandNamesQuery(foodCode: String, locale: String)(implicit conn: java.sql.Connection): Either[Nothing, Unit] = {
    SQL("DELETE FROM brands WHERE food_code={food_code} AND locale_id={locale_id}").on('food_code -> foodCode, 'locale_id -> locale).execute()
    Right(())
  }

  protected def createBrandNamesQuery(brandNames: Map[String, Seq[String]], locale: String)(implicit conn: java.sql.Connection): Either[LocaleOrParentError, Unit] = {
    val brandParams = brandNames.keySet.toSeq.flatMap(k => brandNames(k).map(name => Seq[NamedParameter]('food_code -> k, 'locale_id -> locale, 'name -> name)))

    if (!brandParams.isEmpty) {
      logger.debug("Writing " + brandNames.size + " brands to database")

      val constraintErrors = Map(
        "brands_food_locale_fk" -> UndefinedLocale,
        "brands_food_code_fk" -> ParentRecordNotFound)

      tryWithConstraintsCheck(constraintErrors) {
        batchSql("""INSERT INTO brands VALUES(DEFAULT, {food_code}, {locale_id}, {name})""", brandParams).execute()

        Right(())
      }
    } else {
      logger.debug("createBrandNames request with empty brand names map")
      Right(())
    }
  }
}
