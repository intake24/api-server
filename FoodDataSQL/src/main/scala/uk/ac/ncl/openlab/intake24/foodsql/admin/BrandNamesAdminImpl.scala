package uk.ac.ncl.openlab.intake24.foodsql.admin

import scala.Right

import org.slf4j.LoggerFactory

import com.google.inject.Inject
import com.google.inject.name.Named

import anorm.NamedParameter
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.foodsql.SimpleValidation
import uk.ac.ncl.openlab.intake24.foodsql.modular.BrandNamesAdminQueries
import uk.ac.ncl.openlab.intake24.foodsql.user.BrandNamesUserImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.BrandNamesAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleOrParentError

class BrandNamesAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends BrandNamesAdminImpl

trait BrandNamesAdminImpl extends BrandNamesAdminService with BrandNamesUserImpl with BrandNamesAdminQueries with SimpleValidation {
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
