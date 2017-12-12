package cache

import com.google.inject.Inject
import modules.BasicImpl
import play.api.cache.SyncCacheApi
import uk.ac.ncl.openlab.intake24.api.data.admin._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.{CategoriesAdminService, LocalesAdminService}

case class CachedCategoriesAdminService @Inject()(@BasicImpl service: CategoriesAdminService, localeService: LocalesAdminService, cache: SyncCacheApi)
  extends CategoriesAdminService
    with CacheResult {

  var knownCacheKeys = Set[String]()

  def categoryRecordCacheKey(code: String) = s"CachedCategoriesAdminService.categoryRecordCacheKey.$code"

  def localCategoryRecordCacheKey(code: String, locale: String) = s"CachedCategoriesAdminService.localCategoryRecord.$locale.$code"

  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord] =
    cachePositiveResult(categoryRecordCacheKey(code)) {
      service.getCategoryRecord(code, locale)
    }

  def isCategoryCodeAvailable(code: String): Either[UnexpectedDatabaseError, Boolean] = service.isCategoryCodeAvailable(code)

  def isCategoryCode(code: String): Either[UnexpectedDatabaseError, Boolean] = service.isCategoryCode(code)

  def createMainCategoryRecords(records: Seq[NewMainCategoryRecord]): Either[DependentCreateError, Unit] = service.createMainCategoryRecords(records)

  def createLocalCategoryRecords(localCategoryRecords: Map[String, NewLocalCategoryRecord], locale: String): Either[LocalCreateError, Unit] = service.createLocalCategoryRecords(localCategoryRecords, locale)

  def deleteAllCategories(): Either[UnexpectedDatabaseError, Unit] = service.deleteAllCategories()

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit] = service.deleteCategory(categoryCode)

  def updateMainCategoryRecord(categoryCode: String, categoryMain: MainCategoryRecordUpdate): Either[DependentUpdateError, Unit] = service.updateMainCategoryRecord(categoryCode, categoryMain)

  def updateLocalCategoryRecord(categoryCode: String, categoryLocal: LocalCategoryRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit] = service.updateLocalCategoryRecord(categoryCode, categoryLocal, locale)
}
