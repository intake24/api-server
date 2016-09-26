package uk.ac.ncl.openlab.intake24.foodsql.modular

import anorm.SQL
import anorm.SqlParser.str
import anorm.NamedParameter.symbol
import anorm.sqlToSimple

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
import javax.sql.DataSource
import com.google.inject.Inject
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleOrParentError
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService

trait BrandNamesAdminQueries extends FoodDataSqlService {
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
