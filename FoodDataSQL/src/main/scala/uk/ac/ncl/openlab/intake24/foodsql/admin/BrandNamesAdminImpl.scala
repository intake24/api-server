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
import javax.sql.DataSource
import com.google.inject.Inject
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound

class BrandNamesAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends BrandNamesAdminImpl

trait BrandNamesAdminImpl extends BrandNamesAdminService with SqlDataService with BrandNamesUserImpl with SimpleValidation {
  private val logger = LoggerFactory.getLogger(classOf[BrandNamesAdminImpl])

  def deleteAllBrandNames(locale: String): Either[LocaleError, Unit] = tryWithConnection {
    implicit conn =>
      logger.debug(s"Deleting all brand definitions for locale $locale")

      withTransaction {
        validateLocale(locale).right.flatMap {
          _ =>
            SQL("DELETE FROM brands WHERE locale_id={locale_id}").on('locale_id -> locale).execute()
            Right(())
        }
      }
  }

  def deleteBrandNamesComposable(foodCode: String, locale: String)(implicit conn: java.sql.Connection): Either[DatabaseError, Unit] = {
    SQL("DELETE FROM brands WHERE food_code={food_code} AND locale_id={locale_id}").on('locale_id -> locale).execute()
    Right(())
  }

  def createBrandNamesComposable(brandNames: Map[String, Seq[String]], locale: String)(implicit conn: java.sql.Connection): Either[LocalDependentCreateError, Unit] = {
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

  def updateBrandNamesComposable(foodCode: String, brandNames: Seq[String], locale: String)(implicit conn: java.sql.Connection): Either[LocalDependentCreateError, Unit] =
    for (
      _ <- deleteBrandNamesComposable(foodCode, locale).right;
      _ <- createBrandNamesComposable(Map(foodCode -> brandNames), locale).right
    ) yield ()

  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[LocalDependentCreateError, Unit] = tryWithConnection {
    implicit conn =>
      createBrandNamesComposable(brandNames, locale)
  }
}
