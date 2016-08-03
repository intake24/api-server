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
import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.services.UpdateResult
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.services.Success
import uk.ac.ncl.openlab.intake24.services.LocaleManagementService
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.services.NewFood
import uk.ac.ncl.openlab.intake24.services.InvalidRequest
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.services.NewCategory


import modules.UncachedImpl
import uk.ac.ncl.openlab.intake24.FoodGroup
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.AssociatedFood

case class CachedAdminFoodDataService @Inject() (@UncachedImpl service: AdminFoodDataService, localeService: LocaleManagementService, cache: CacheApi) extends AdminFoodDataService 
  with ProblemCheckerCache with AssociatedFoodsCache {

  def uncategorisedFoodsCacheKey(locale: String) = s"AdminFoodDataService.uncategorisedFoods.$locale"

  def uncategorisedFoods(locale: String): Seq[FoodHeader] = cache.getOrElse(uncategorisedFoodsCacheKey(locale)) {
    service.uncategorisedFoods(locale)
  }

  def invalidateUncategorisedFoods(locale: String) = cache.remove(uncategorisedFoodsCacheKey(locale))

  def rootCategoriesCacheKey(locale: String) = s"AdminFoodDataService.rootCategories.$locale"

  def rootCategories(locale: String): Seq[CategoryHeader] = cache.getOrElse(rootCategoriesCacheKey(locale)) {
    service.rootCategories(locale)
  }

  def invalidateRootCategories(locale: String) = cache.remove(rootCategoriesCacheKey(locale))

  def categoryContentsCacheKey(code: String, locale: String) = s"AdminFoodDataService.rootCategories.$locale.$code"

  def categoryContents(code: String, locale: String): CategoryContents = cache.getOrElse(categoryContentsCacheKey(code, locale)) {
    service.categoryContents(code, locale)
  }

  def invalidateCategoryContents(code: String, locale: String) = cache.remove(categoryContentsCacheKey(code, locale))

  def foodRecordCacheKey(code: String, locale: String) = s"AdminFoodDataService.foodRecord.$locale.$code"

  def foodRecord(code: String, locale: String): Either[CodeError, FoodRecord] = {
    val key = foodRecordCacheKey(code, locale)

    cache.get[FoodRecord](key) match {
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

  def invalidateFoodRecord(code: String, locale: String) = cache.remove(foodRecordCacheKey(code, locale))

  def isCategoryCode(code: String): Boolean = service.isCategoryCode(code)

  def isFoodCode(code: String): Boolean = service.isFoodCode(code)

  def foodParentCategoriesCacheKey(code: String, locale: String) = s"AdminFoodDataService.foodParentCategories.$locale.$code"

  def foodParentCategories(code: String, locale: String): Seq[CategoryHeader] = cache.getOrElse(foodParentCategoriesCacheKey(code, locale)) {
    service.foodParentCategories(code, locale)
  }

  def invalidateFoodParentCategories(code: String, locale: String) = cache.remove(foodParentCategoriesCacheKey(code, locale))

  def foodAllCategoriesGlobalCacheKey(code: String) = s"AdminFoodDataService.foodAllCategoriesGlobal.$code"

  def foodAllCategories(code: String): Seq[String] = cache.getOrElse(foodAllCategoriesGlobalCacheKey(code)) {
    service.foodAllCategories(code)
  }

  def foodAllCategoriesLocalCacheKey(code: String, locale: String) = s"AdminFoodDataService.foodAllCategoriesLocal.$locale.$code"

  def foodAllCategories(code: String, locale: String): Seq[CategoryHeader] = cache.getOrElse(foodAllCategoriesLocalCacheKey(code, locale)) {
    service.foodAllCategories(code, locale)
  }

  def invalidateFoodAllCategories(code: String, locale: String) = {
    cache.remove(foodAllCategoriesGlobalCacheKey(code))
    cache.remove(foodAllCategoriesLocalCacheKey(code, locale))
  }

  def categoryParentCategoriesCacheKey(code: String, locale: String) = s"AdminFoodDataService.categoryParentCategories.$locale.$code"

  def categoryParentCategories(code: String, locale: String): Seq[CategoryHeader] = cache.getOrElse(categoryParentCategoriesCacheKey(code, locale)) {
    service.categoryParentCategories(code, locale)
  }

  def invalidateCategoryParentCategories(code: String, locale: String) = {
    cache.remove(categoryParentCategoriesCacheKey(code, locale))
  }

  def categoryAllCategoriesGlobalCacheKey(code: String) = s"AdminFoodDataService.categoryAllCategoriesGlobal.$code"

  def categoryAllCategories(code: String): Seq[String] = cache.getOrElse(categoryAllCategoriesGlobalCacheKey(code)) {
    service.categoryAllCategories(code)
  }

  def categoryAllCategoriesLocalCacheKey(code: String, locale: String) = s"AdminFoodDataService.categoryAllCategoriesLocal.$locale.$code"

  def categoryAllCategories(code: String, locale: String): Seq[CategoryHeader] = cache.getOrElse(categoryAllCategoriesLocalCacheKey(code, locale)) {
    service.categoryAllCategories(code, locale)
  }

  def invalidateCategoryAllCategories(code: String, locale: String) = {
    cache.remove(categoryAllCategoriesGlobalCacheKey(code))
    cache.remove(categoryAllCategoriesLocalCacheKey(code, locale))
  }

  def categoryRecordCacheKey(code: String, locale: String) = s"AdminFoodDataService.categoryRecord.$locale.$code"

  def categoryRecord(code: String, locale: String): Either[CodeError, CategoryRecord] = {
    val key = categoryRecordCacheKey(code, locale)

    cache.get[CategoryRecord](key) match {
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

  def invalidateCategoryRecord(code: String, locale: String) =
    cache.remove(categoryRecordCacheKey(code, locale))

  def allAsServedSetsCacheKey() = s"AdminFoodDataService.allAsServedSets"

  def allAsServedSets() = cache.getOrElse(allAsServedSetsCacheKey()) {
    service.allAsServedSets()
  }

  def allGuideImagesCacheKey() = s"AdminFoodDataService.allGuideImages"

  def allGuideImages() = cache.getOrElse(allGuideImagesCacheKey()) {
    service.allGuideImages()
  }

  def allDrinkwareCacheKey() = s"AdminFoodDataService.allDrinkware"

  def allDrinkware() = cache.getOrElse(allDrinkwareCacheKey()) {
    service.allDrinkware()
  }

  def allFoodGroupsCacheKey(locale: String) = s"AdminFoodDataService.allFoodGroups.$locale"

  def allFoodGroups(locale: String) = cache.getOrElse(allFoodGroupsCacheKey(locale)) {
    service.allFoodGroups(locale)
  }

  def foodGroupCacheKey(code: Int, locale: String) = s"AdminFoodDataService.foodGroup.$locale.$code"

  def foodGroup(code: Int, locale: String) = {
    val key = foodGroupCacheKey(code, locale)
    cache.get[FoodGroup](key) match {

      case Some(group) => Some(group)
      case None => service.foodGroup(code, locale) match {
        case x @ Some(group) => {
          cache.set(key, group)
          x
        }
        case x => x
      }
    }
  }

  def nutrientTablesCacheKey() = s"AdminFoodDataService.nutrientTables"

  def nutrientTables() = cache.getOrElse(nutrientTablesCacheKey()) {
    service.nutrientTables()
  }

  // These are intentionally uncached

  def searchFoods(searchTerm: String, locale: String): Seq[FoodHeader] = service.searchFoods(searchTerm, locale)

  def searchCategories(searchTerm: String, locale: String): Seq[CategoryHeader] = service.searchCategories(searchTerm, locale)

  // Write
  
  def invalidateProblemsForFood(code: String, locale: String) = {
    
    invalidateFoodProblems(code, locale)
    
    foodAllCategories(code, locale).foreach {
      header =>
        invalidateRecursiveCategoryProblems(header.code, locale)
    }
  }

  def invalidateProblemsForCategory(code: String, locale: String) = {
    invalidateCategoryProblems(code, locale)
    
    categoryAllCategories(code, locale).foreach {
      header =>
        invalidateRecursiveCategoryProblems(code, locale)
    }
  }

  def updateFoodBase(foodCode: String, foodBase: MainFoodRecord): UpdateResult = {
    val result = service.updateFoodBase(foodCode, foodBase)

    if (result == Success) {

      val categories = foodAllCategories(foodCode)

      localeService.list.foreach {
        locale =>
          invalidateFoodRecord(foodCode, locale.id)
          invalidateProblemsForFood(foodCode, locale.id)
      }
    }

    result
  }

  def updateFoodLocal(foodCode: String, locale: String, foodLocal: LocalFoodRecord): UpdateResult = {
    val result = service.updateFoodLocal(foodCode, locale, foodLocal)

    if (result == Success) {
      invalidateFoodRecord(foodCode, locale)
      invalidateProblemsForFood(foodCode, locale)
    }

    result
  }

  // Intentionally uncached
  def isFoodCodeAvailable(code: String): Boolean = service.isFoodCodeAvailable(code)

  // Does not affect cache
  def createFood(newFood: NewFood): UpdateResult = service.createFood(newFood)

  // Does not affect cache
  def createFoodWithTempCode(newFood: NewFood): Either[InvalidRequest, String] = service.createFoodWithTempCode(newFood)

  def deleteFood(foodCode: String): UpdateResult = {
    val categories = foodAllCategories(foodCode)

    val result = service.deleteFood(foodCode)

    if (result == Success) {

      localeService.list.foreach {
        locale =>
          invalidateFoodRecord(foodCode, locale.id)
          invalidateProblemsForFood(foodCode, locale.id)
          invalidateFoodParentCategories(foodCode, locale.id)

          if (categories.isEmpty)
            invalidateUncategorisedFoods(locale.id)

          categories.foreach {
            categoryCode =>
              invalidateProblemsForCategory(categoryCode, locale.id)
              invalidateCategoryContents(categoryCode, locale.id)
          }
      }
    }

    result
  }

  def updateCategoryBase(categoryCode: String, categoryBase: MainCategoryRecord): UpdateResult = {
    val result = service.updateCategoryBase(categoryCode, categoryBase)

    if (result == Success) {

      val categories = categoryAllCategories(categoryCode)

      localeService.list.foreach {
        locale =>
          invalidateCategoryRecord(categoryCode, locale.id)
          invalidateProblemsForCategory(categoryCode, locale.id)
      }
    }

    result
  }

  def updateCategoryLocal(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): UpdateResult = {
    val result = service.updateCategoryLocal(categoryCode, locale, categoryLocal)

    if (result == Success) {
      invalidateCategoryRecord(categoryCode, locale)
      invalidateProblemsForCategory(categoryCode, locale)
    }

    result
  }

  // Intentionally uncached
  def isCategoryCodeAvailable(code: String): Boolean = service.isCategoryCodeAvailable(code)

  // Does not affect cache
  def createCategory(newCategory: NewCategory): UpdateResult = service.createCategory(newCategory)

  def deleteCategory(categoryCode: String): UpdateResult = {
    val categories = categoryAllCategories(categoryCode)

    val result = service.deleteCategory(categoryCode)

    if (result == Success) {

      localeService.list.foreach {
        locale =>
          invalidateCategoryRecord(categoryCode, locale.id)
          invalidateProblemsForCategory(categoryCode, locale.id)
          invalidateCategoryParentCategories(categoryCode, locale.id)
          invalidateRootCategories(locale.id)

          categories.foreach {
            categoryCode =>
              invalidateProblemsForCategory(categoryCode, locale.id)
              invalidateCategoryContents(categoryCode, locale.id)
          }
      }
    }

    result
  }

  def addFoodToCategory(categoryCode: String, foodCode: String): UpdateResult = {
    val result = service.addFoodToCategory(categoryCode, foodCode)

    if (result == Success) {
      localeService.list.foreach {
        locale =>
          invalidateFoodParentCategories(foodCode, locale.id)
          invalidateCategoryContents(categoryCode, locale.id)
          invalidateUncategorisedFoods(locale.id)

          invalidateProblemsForFood(foodCode, locale.id)
          invalidateProblemsForCategory(categoryCode, locale.id)
      }
    }

    result
  }

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): UpdateResult = {
    val result = service.addSubcategoryToCategory(categoryCode, subcategoryCode)

    if (result == Success) {
      localeService.list.foreach {
        locale =>
          invalidateRootCategories(locale.id)

          invalidateCategoryParentCategories(subcategoryCode, locale.id)
          invalidateCategoryContents(categoryCode, locale.id)

          invalidateProblemsForCategory(subcategoryCode, locale.id)
          invalidateProblemsForCategory(categoryCode, locale.id)
      }
    }
    
    result
  }

  def removeFoodFromCategory(categoryCode: String, foodCode: String): UpdateResult = {
    val result = service.removeFoodFromCategory(categoryCode, foodCode)

    if (result == Success) {
      localeService.list.foreach {
        locale =>
          invalidateFoodAllCategories(foodCode, locale.id)
          invalidateFoodParentCategories(foodCode, locale.id)
          invalidateCategoryContents(categoryCode, locale.id)
          invalidateUncategorisedFoods(locale.id)

          invalidateProblemsForFood(foodCode, locale.id)
          invalidateProblemsForCategory(categoryCode, locale.id)
      }
    }

    result
  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String): UpdateResult = {
    val result = service.removeSubcategoryFromCategory(categoryCode, subcategoryCode)

    if (result == Success) {
      localeService.list.foreach {
        locale =>
          invalidateCategoryAllCategories(subcategoryCode, locale.id)
          invalidateCategoryParentCategories(subcategoryCode, locale.id)
          invalidateCategoryContents(categoryCode, locale.id)
          invalidateRootCategories(locale.id)
          
          invalidateProblemsForCategory(categoryCode, locale.id)
          invalidateProblemsForCategory(subcategoryCode, locale.id)
      }
    }
    
    result
  }
  
  def associatedFoods(foodCode: String, locale: String): Either[CodeError, Seq[AssociatedFoodWithHeader]] = cache.getOrElse(associatedFoodsCacheKey(foodCode, locale)) {
    service.associatedFoods(foodCode, locale)
  }

  def updateAssociatedFoods(foodCode: String, locale: String, associatedFoods: Seq[AssociatedFood]): UpdateResult = {
    val result = service.updateAssociatedFoods(foodCode, locale, associatedFoods)
    
    if (result == Success)
      invalidateAssociatedFoods(foodCode, locale)
    
    result
  }

}