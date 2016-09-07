package cache

import com.google.inject.Inject

import modules.UncachedImpl
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
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

case class CachedCategoriesAdminService @Inject() (@UncachedImpl service: CategoriesAdminService, localeService: LocalesAdminService, cache: CacheApi)
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

  def createCategory(newCategory: NewCategory): Either[CreateError, Unit] = service.createCategory(newCategory)
  
  def createCategories(newCategories: Seq[NewCategory]): Either[CreateError, Unit] = service.createCategories(newCategories)
    
  def createLocalCategories(localCategoryRecords: Map[String, LocalCategoryRecord], locale: String): Either[CreateError, Unit] = service.createLocalCategories(localCategoryRecords, locale)

  def deleteAllCategories(): Either[DatabaseError, Unit] = service.deleteAllCategories()
  
  def deleteCategory(categoryCode: String): Either[DeleteError, Unit] = service.deleteCategory(categoryCode)

  def updateMainCategoryRecord(categoryCode: String, categoryMain: MainCategoryRecord): Either[UpdateError, Unit] = service.updateMainCategoryRecord(categoryCode, categoryMain) 
    
  def updateLocalCategoryRecord(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[LocalUpdateError, Unit] = service.updateLocalCategoryRecord(categoryCode, locale, categoryLocal)

  def addFoodToCategory(categoryCode: String, foodCode: String): Either[ParentError, Unit] = service.addFoodToCategory(categoryCode, foodCode)
  
  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[ParentError, Unit] = service.addSubcategoryToCategory(categoryCode, subcategoryCode)
  
  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[LookupError, Unit] = service.removeFoodFromCategory(categoryCode, foodCode)
  
  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): Either[LookupError, Unit] = service.removeSubcategoryFromCategory(categoryCode, foodCode)
}
