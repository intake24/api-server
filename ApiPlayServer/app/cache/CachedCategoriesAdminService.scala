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
import uk.ac.ncl.openlab.intake24.CategoryRecord
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.LocalesAdminService
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.NewLocalCategoryRecord
import uk.ac.ncl.openlab.intake24.MainCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentUpdateError
import uk.ac.ncl.openlab.intake24.LocalCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.NewMainCategoryRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalCreateError

case class CachedCategoriesAdminService @Inject() (@BasicImpl service: CategoriesAdminService, localeService: LocalesAdminService, cache: CacheApi)
    extends CategoriesAdminService
    with CacheResult {

  var knownCacheKeys = Set[String]()

  def categoryRecordCacheKey(code: String) = s"CachedCategoriesAdminService.categoryRecordCacheKey.$code"

  def localCategoryRecordCacheKey(code: String, locale: String) = s"CachedCategoriesAdminService.localCategoryRecord.$locale.$code"

  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord] =
    cachePositiveResult(categoryRecordCacheKey(code)) {
      service.getCategoryRecord(code, locale)
    }

  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean] = service.isCategoryCodeAvailable(code)

  def isCategoryCode(code: String): Either[DatabaseError, Boolean] = service.isCategoryCode(code)

  def createMainCategoryRecords(records: Seq[NewMainCategoryRecord]): Either[DependentCreateError, Unit] = service.createMainCategoryRecords(records)

  def createLocalCategoryRecords(localCategoryRecords: Map[String, NewLocalCategoryRecord], locale: String): Either[LocalCreateError, Unit] = service.createLocalCategoryRecords(localCategoryRecords, locale)

  def deleteAllCategories(): Either[DatabaseError, Unit] = service.deleteAllCategories()

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit] = service.deleteCategory(categoryCode)

  def updateMainCategoryRecord(categoryCode: String, categoryMain: MainCategoryRecordUpdate): Either[DependentUpdateError, Unit] = service.updateMainCategoryRecord(categoryCode, categoryMain)

  def updateLocalCategoryRecord(categoryCode: String, categoryLocal: LocalCategoryRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit] = service.updateLocalCategoryRecord(categoryCode, categoryLocal, locale)
}
