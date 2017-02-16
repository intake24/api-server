package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.errors.{LocalDependentCreateError, LocaleError}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.BrandNamesService

trait BrandNamesAdminService extends BrandNamesService {

  def deleteAllBrandNames(locale: String): Either[LocaleError, Unit]

  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[LocalDependentCreateError, Unit]
}
