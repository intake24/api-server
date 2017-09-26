package cache

import com.google.inject.Inject
import modules.BasicImpl
import play.api.cache.SyncCacheApi
import uk.ac.ncl.openlab.intake24.errors.{LocalDependentCreateError, LocalLookupError, LocaleError}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.BrandNamesAdminService

case class CachedBrandNamesAdminService @Inject()(@BasicImpl service: BrandNamesAdminService, cache: SyncCacheApi)
  extends BrandNamesAdminService
    with CacheResult {

  var knownCacheKeys = Set[String]()

  def deleteAllBrandNames(locale: String): Either[LocaleError, Unit] = notSupported

  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[LocalDependentCreateError, Unit] = notSupported

  def getBrandNames(foodCode: String, locale: String): Either[LocalLookupError, Seq[String]] = service.getBrandNames(foodCode, locale)
}
