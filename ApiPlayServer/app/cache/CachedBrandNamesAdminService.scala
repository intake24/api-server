package cache

import com.google.inject.Inject
import modules.BasicImpl
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.BrandNamesAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.{LocalDependentCreateError, LocalLookupError, LocaleError}

case class CachedBrandNamesAdminService @Inject() (@BasicImpl service: BrandNamesAdminService, cache: CacheApi)
    extends BrandNamesAdminService
    with CacheResult {

  var knownCacheKeys = Set[String]()

  def deleteAllBrandNames(locale: String): Either[LocaleError, Unit] = notSupported

  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[LocalDependentCreateError, Unit] = notSupported

  def getBrandNames(foodCode: String, locale: String): Either[LocalLookupError, Seq[String]] = service.getBrandNames(foodCode, locale)
}
