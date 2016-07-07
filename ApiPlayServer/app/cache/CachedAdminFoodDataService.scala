package cache

import uk.ac.ncl.openlab.intake24.services.AdminFoodDataService
import com.google.inject.Inject
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.CategoryContents
import uk.ac.ncl.openlab.intake24.services.CodeError
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.CategoryRecord

class CachedAdminFoodDataService @Inject() (service: AdminFoodDataService, cache: CacheApi) extends AdminFoodDataService {

  def uncategorisedFoodsCacheKey(locale: String) = s"AdminFoodDataService.uncategorisedFoods.$locale"

  def uncategorisedFoods(locale: String): Seq[FoodHeader] = cache.getOrElse(uncategorisedFoodsCacheKey(locale)) {
    service.uncategorisedFoods(locale)
  }

  def rootCategoriesCacheKey(locale: String) = s"AdminFoodDataService.rootCategories.$locale"

  def rootCategories(locale: String): Seq[CategoryHeader] = cache.getOrElse(rootCategoriesCacheKey(locale)) {
    service.rootCategories(locale)
  }

  def categoryContentsCacheKey(code: String, locale: String) = s"AdminFoodDataService.rootCategories.$locale.$code"

  def categoryContents(code: String, locale: String): CategoryContents = cache.getOrElse(categoryContentsCacheKey(code, locale)) {
    service.categoryContents(code, locale)
  }

  def foodRecordCacheKey(code: String, locale: String) = s"AdminFoodDataService.foodRecord.$locale.$code"

  def foodRecord(code: String, locale: String): Either[CodeError, FoodRecord] = {
    val key = foodRecordCacheKey(code, locale)

    cache.get(key) match {
      case Some(record) => Right(record)
      case None => service.foodRecord(code, locale) match {
        case result @ Right(record) => {
          cache.set(key, record)
          result
        }
        case result => result
      }
    }
  }

  def isCategoryCode(code: String): Boolean = service.isCategoryCode(code)

  def isFoodCode(code: String): Boolean = service.isFoodCode(code)

  def foodParentCategoriesCacheKey(code: String, locale: String) = s"AdminFoodDataService.foodParentCategories.$locale.$code"

  def foodParentCategories(code: String, locale: String): Seq[CategoryHeader] = cache.getOrElse(foodParentCategoriesCacheKey(code, locale)) {
    service.foodParentCategories(code, locale)
  }

  def foodAllCategoriesGlobalCacheKey(code: String) = s"AdminFoodDataService.foodAllCategoriesGlobal.$code"

  def foodAllCategories(code: String): Seq[String] = cache.getOrElse(foodAllCategoriesGlobalCacheKey(code)) {
    service.foodAllCategories(code)
  }

  def foodAllCategoriesLocalCacheKey(code: String, locale: String) = s"AdminFoodDataService.foodAllCategoriesLocal.$locale.$code"

  def foodAllCategories(code: String, locale: String): Seq[CategoryHeader] = cache.getOrElse(foodAllCategoriesLocalCacheKey(code, locale)) {
    service.foodAllCategories(code, locale)
  }

  def categoryParentCategoriesCacheKey(code: String, locale: String) = s"AdminFoodDataService.categoryParentCategories.$locale.$code"

  def categoryParentCategories(code: String, locale: String): Seq[CategoryHeader] = cache.getOrElse(categoryParentCategoriesCacheKey(code, locale)) {
    service.categoryParentCategories(code, locale)
  }

  def categoryAllCategoriesGlobalCacheKey(code: String) = s"AdminFoodDataService.categoryAllCategoriesGlobal.$code"

  def categoryAllCategories(code: String): Seq[String] = cache.getOrElse(categoryAllCategoriesGlobalCacheKey(code)) {
    service.categoryAllCategories(code)
  }

  def categoryAllCategoriesLocalCacheKey(code: String, locale: String) = s"AdminFoodDataService.categoryAllCategoriesLocal.$locale.$code"

  def categoryAllCategories(code: String, locale: String): Seq[CategoryHeader] = cache.getOrElse(categoryAllCategoriesLocalCacheKey(code, locale)) {
    service.categoryAllCategories(code, locale)
  }

  def categoryRecordCacheKey(code: String, locale: String) = s"AdminFoodDataService.categoryRecord.$locale.$code"

  def categoryRecord(code: String, locale: String): Either[CodeError, CategoryRecord] = {
    val key = categoryRecordCacheKey(code, locale)

    cache.get(key) match {
      case Some(record) => Right(record)
      case None => service.categoryRecord(code, locale) match {
        case result @ Right(record) => {
          cache.set(key, record)
          result
        }
        case result => result
      }
    }
  }

  def allAsServedSets(): Seq[AsServedHeader]

  def allGuideImages(): Seq[GuideHeader]

  def allDrinkware(): Seq[DrinkwareHeader]

  def allFoodGroups(locale: String): Seq[FoodGroup]

  def foodGroup(code: Int, locale: String): Option[FoodGroup]

  def nutrientTables(): Seq[NutrientTable]

  def searchFoods(searchTerm: String, locale: String): Seq[FoodHeader]

  def searchCategories(searchTerm: String, locale: String): Seq[CategoryHeader]

  // Write

  def updateFoodBase(foodCode: String, foodBase: MainFoodRecord): UpdateResult

  def updateFoodLocal(foodCode: String, locale: String, foodLocal: LocalFoodRecord): UpdateResult

  def isFoodCodeAvailable(code: String): Boolean

  def createFood(newFood: NewFood): UpdateResult

  def createFoodWithTempCode(newFood: NewFood): Either[InvalidRequest, String]

  def deleteFood(foodCode: String): UpdateResult

  def updateCategoryBase(categoryCode: String, categoryBase: MainCategoryRecord): UpdateResult

  def updateCategoryLocal(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): UpdateResult

  def isCategoryCodeAvailable(code: String): Boolean

  def createCategory(newCategory: NewCategory): UpdateResult

  def deleteCategory(categoryCode: String): UpdateResult

  def addFoodToCategory(categoryCode: String, foodCode: String): UpdateResult

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): UpdateResult

  def removeFoodFromCategory(categoryCode: String, foodCode: String): UpdateResult

  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): UpdateResult

  def updateAssociatedFoods(foodCode: String, locale: String, associatedFoods: Seq[AssociatedFood]): UpdateResult

}