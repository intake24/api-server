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

case class CachedCategoriesAdminService @Inject() (@UncachedImpl service: CachedCategoriesAdminService, localeService: LocalesAdminService, cache: CacheApi)
    extends AsServedImageAdminService
    with CacheResult
    with FoodBrowsingCacheKeys 
    with ProblemCheckerCacheKeys
    {

  def categoryRecordCacheKey(code: String, locale: String) = s"CachedCategoriesAdminService.categoryRecord.$locale.$code"

  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord] =
    cachePositiveResult(categoryRecordCacheKey(code, locale)) {
      service.getCategoryRecord(code, locale)
    }

  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean] = service.isCategoryCodeAvailable(code)
  def isCategoryCode(code: String): Either[DatabaseError, Boolean] = service.isCategoryCode(code)

  def createCategory(newCategory: NewCategory): Either[CreateError, Unit] = service.createCategory(newCategory).right.map {
    _ =>
      localeService.listLocales().right.map {
        locales =>
          locales.keySet.foreach {
            locale =>
              invalidateRootCategories(locale)
          }
      }
  }

  def createCategories(newCategories: Seq[NewCategory]): Either[CreateError, Unit] = service.createCategories(newCategories).right.map {
    _ =>
      localeService.listLocales().right.map {
        locales =>
          locales.keySet.foreach {
            locale =>
              invalidateRootCategories(locale)
          }
      }
  }

  def createLocalCategories(localCategoryRecords: Map[String, LocalCategoryRecord], locale: String): Either[CreateError, Unit] = notSupported 

  def deleteAllCategories(): Either[DatabaseError, Unit] = notSupported 
  
  def deleteCategory(categoryCode: String): Either[DeleteError, Unit] = 

  def updateMainCategoryRecord(categoryCode: String, categoryMain: MainCategoryRecord): Either[UpdateError, Unit]
  def updateLocalCategoryRecord(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[LocalUpdateError, Unit]

  def addFoodToCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]
  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit]
  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]
  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]
}
