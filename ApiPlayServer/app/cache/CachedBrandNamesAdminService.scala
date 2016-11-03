package cache

import com.google.inject.Inject

import modules.BasicImpl
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.AsServedHeader

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.BrandNamesAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentCreateError

case class CachedBrandNamesAdminService @Inject() (@BasicImpl service: BrandNamesAdminService, cache: CacheApi)
    extends BrandNamesAdminService
    with CacheResult {

  var knownCacheKeys = Set[String]()

  def deleteAllBrandNames(locale: String): Either[LocaleError, Unit] = notSupported

  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[LocalDependentCreateError, Unit] = notSupported

  def getBrandNames(foodCode: String, locale: String): Either[LocalLookupError, Seq[String]] = service.getBrandNames(foodCode, locale)
}
