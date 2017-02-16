package uk.ac.ncl.openlab.intake24.foodsql.admin

import javax.sql.DataSource

import anorm.{SQL, sqlToSimple}
import com.google.inject.Inject
import com.google.inject.name.Named
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{LocaleError, LocaleOrParentError}
import uk.ac.ncl.openlab.intake24.foodsql.SimpleValidation
import uk.ac.ncl.openlab.intake24.foodsql.modular.BrandNamesAdminQueries
import uk.ac.ncl.openlab.intake24.foodsql.user.BrandNamesUserImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.BrandNamesAdminService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

class BrandNamesAdminStandaloneImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource) extends BrandNamesAdminImpl

trait BrandNamesAdminImpl extends BrandNamesAdminService with BrandNamesUserImpl with BrandNamesAdminQueries with SqlDataService with SimpleValidation {
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

  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[LocaleOrParentError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        createBrandNamesQuery(brandNames, locale)
      }
  }
}
