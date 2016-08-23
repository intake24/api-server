package cache

import com.google.inject.Inject
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.CategoryContents

import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.CategoryRecord
import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.MainFoodRecord

import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord

import modules.UncachedImpl

import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import uk.ac.ncl.openlab.intake24.NewCategory

case class CachedFoodDatabaseAdminService @Inject() (@UncachedImpl service: FoodDatabaseAdminService, cache: CacheApi) extends FoodDatabaseAdminService
    with ProblemCheckerCache with AssociatedFoodsCache {

  def uncategorisedFoodsCacheKey(locale: String) = s"AdminFoodDataService.uncategorisedFoods.$locale"

  def cacheResult[E, T](key: String)(block: => Either[E, T])(implicit ev: scala.reflect.ClassTag[T]) = cache.get[T](key) match {
    case Some(cached) => Right(cached)
    case None => block match {
      case Right(result) => {
        cache.set(key, result)
        Right(result)
      }
      case error => error
    }
  }

  def getUncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]] = cacheResult(uncategorisedFoodsCacheKey(locale)) {
    service.getUncategorisedFoods(locale)
  }

  def invalidateUncategorisedFoods(locale: String) = cache.remove(uncategorisedFoodsCacheKey(locale))

  def rootCategoriesCacheKey(locale: String) = s"AdminFoodDataService.rootCategories.$locale"

  def getRootCategories(locale: String): Either[LocaleError, Seq[CategoryHeader]] = cacheResult(rootCategoriesCacheKey(locale)) {
    service.getRootCategories(locale)
  }

  def invalidateRootCategories(locale: String) = cache.remove(rootCategoriesCacheKey(locale))

  def categoryContentsCacheKey(code: String, locale: String) = s"AdminFoodDataService.rootCategories.$locale.$code"

  def getCategoryContents(code: String, locale: String): Either[LocalLookupError, CategoryContents] = cacheResult(categoryContentsCacheKey(code, locale)) {
    service.getCategoryContents(code, locale)
  }

  def invalidateCategoryContents(code: String, locale: String) = cache.remove(categoryContentsCacheKey(code, locale))

  def foodRecordCacheKey(code: String, locale: String) = s"AdminFoodDataService.foodRecord.$locale.$code"

  def getFoodRecord(code: String, locale: String): Either[LocalLookupError, FoodRecord] = cacheResult(foodRecordCacheKey(code, locale)) {
    service.getFoodRecord(code, locale)
  }

  def invalidateFoodRecord(code: String, locale: String) = cache.remove(foodRecordCacheKey(code, locale))

  def isCategoryCode(code: String): Either[LookupError, Boolean] = service.isCategoryCode(code)

  def isFoodCode(code: String): Either[LookupError, Boolean] = service.isFoodCode(code)

  def foodParentCategoriesCacheKey(code: String, locale: String) = s"AdminFoodDataService.foodParentCategories.$locale.$code"

  def getFoodParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = cacheResult(foodParentCategoriesCacheKey(code, locale)) {
    service.getFoodParentCategories(code, locale)
  }

  def invalidateFoodParentCategories(code: String, locale: String) = cache.remove(foodParentCategoriesCacheKey(code, locale))

  def foodAllCategoriesGlobalCacheKey(code: String) = s"AdminFoodDataService.foodAllCategoriesGlobal.$code"

  def getFoodAllCategoriesCodes(code: String): Either[LookupError, Set[String]] = cache.getOrElse(foodAllCategoriesGlobalCacheKey(code)) {
    service.getFoodAllCategoriesCodes(code)
  }

  def foodAllCategoriesLocalCacheKey(code: String, locale: String) = s"AdminFoodDataService.foodAllCategoriesLocal.$locale.$code"

  def getFoodAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = cacheResult(foodAllCategoriesLocalCacheKey(code, locale)) {
    service.getFoodAllCategoriesHeaders(code, locale)
  }

  def invalidateFoodAllCategories(code: String, locale: String) = {
    cache.remove(foodAllCategoriesGlobalCacheKey(code))
    cache.remove(foodAllCategoriesLocalCacheKey(code, locale))
  }

  def categoryParentCategoriesCacheKey(code: String, locale: String) = s"AdminFoodDataService.categoryParentCategories.$locale.$code"

  def getCategoryParentCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = cacheResult(categoryParentCategoriesCacheKey(code, locale)) {
    service.getCategoryParentCategories(code, locale)
  }

  def invalidateCategoryParentCategories(code: String, locale: String) = {
    cache.remove(categoryParentCategoriesCacheKey(code, locale))
  }

  def categoryAllCategoriesGlobalCacheKey(code: String) = s"AdminFoodDataService.categoryAllCategoriesGlobal.$code"

  def getCategoryAllCategoriesCodes(code: String): Either[LookupError, Set[String]] = cacheResult(categoryAllCategoriesGlobalCacheKey(code)) {
    service.getCategoryAllCategoriesCodes(code)
  }

  def categoryAllCategoriesLocalCacheKey(code: String, locale: String) = s"AdminFoodDataService.categoryAllCategoriesLocal.$locale.$code"

  def getCategoryAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] = cacheResult(categoryAllCategoriesLocalCacheKey(code, locale)) {
    service.getCategoryAllCategoriesHeaders(code, locale)
  }

  def invalidateCategoryAllCategories(code: String, locale: String) = {
    cache.remove(categoryAllCategoriesGlobalCacheKey(code))
    cache.remove(categoryAllCategoriesLocalCacheKey(code, locale))
  }

  def categoryRecordCacheKey(code: String, locale: String) = s"AdminFoodDataService.categoryRecord.$locale.$code"

  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord] = cacheResult(categoryRecordCacheKey(code, locale)) {
    service.getCategoryRecord(code, locale)
  }

  def invalidateCategoryRecord(code: String, locale: String) =
    cache.remove(categoryRecordCacheKey(code, locale))

  def allAsServedSetsCacheKey() = s"AdminFoodDataService.allAsServedSets"

  def listAsServedSets(): Either[DatabaseError, Map[String, AsServedHeader]] = cacheResult(allAsServedSetsCacheKey()) {
    service.listAsServedSets()
  }

  def allGuideImagesCacheKey() = s"AdminFoodDataService.allGuideImages"

  def listGuideImages() = cacheResult(allGuideImagesCacheKey()) {
    service.listGuideImages()
  }

  def allDrinkwareCacheKey() = s"AdminFoodDataService.allDrinkware"

  def listDrinkwareSets() = cacheResult(allDrinkwareCacheKey()) {
    service.listDrinkwareSets()
  }

  def allFoodGroupsCacheKey(locale: String) = s"AdminFoodDataService.allFoodGroups.$locale"

  def listFoodGroups(locale: String) = cacheResult(allFoodGroupsCacheKey(locale)) {
    service.listFoodGroups(locale)
  }

  def foodGroupCacheKey(code: Int, locale: String) = s"AdminFoodDataService.foodGroup.$locale.$code"

  def getrFoodGroup(code: Int, locale: String) = cacheResult(foodGroupCacheKey(code, locale)) {
    service.getFoodGroup(code, locale)
  }

  def nutrientTablesCacheKey() = s"AdminFoodDataService.nutrientTables"

  def listNutrientTables() = cacheResult(nutrientTablesCacheKey()) {
    service.listNutrientTables()
  }

  // These are intentionally uncached

  def searchFoods(searchTerm: String, locale: String): Either[LocaleError, Seq[FoodHeader]] = service.searchFoods(searchTerm, locale)

  def searchCategories(searchTerm: String, locale: String): Either[LocaleError, Seq[CategoryHeader]] = service.searchCategories(searchTerm, locale)

  // Write

  def invalidateProblemsForFood(code: String, locale: String) = {

    invalidateFoodProblems(code, locale)

    getFoodAllCategoriesCodes(code).right.get.foreach {
      code =>
        invalidateRecursiveCategoryProblems(code, locale)
    }
  }

  def invalidateProblemsForCategory(code: String, locale: String) = {
    invalidateCategoryProblems(code, locale)

    getCategoryAllCategoriesCodes(code).right.get.foreach {
      code =>
        invalidateRecursiveCategoryProblems(code, locale)
    }
  }

  def updateMainFoodRecord(foodCode: String, foodBase: MainFoodRecord): Either[UpdateError, Unit] = {
    service.updateMainFoodRecord(foodCode, foodBase).right.map {
      _ =>

        val codes = service.getFoodAllCategoriesCodes(foodCode).right.get
        val locales = service.listLocales().right.get

        locales.keySet.foreach {
          locale_id =>
            invalidateFoodRecord(foodCode, locale_id)
            invalidateProblemsForFood(foodCode, locale_id)

            codes.foreach(invalidateRecursiveCategoryProblems(_, locale_id))
        }
    }
  }

  def updateFoodLocal(foodCode: String, locale: String, foodLocal: LocalFoodRecord): Either[LocalUpdateError, Unit] = {
    service.updateLocalFoodRecord(foodCode, locale, foodLocal).right.map {
      _ =>

        val codes = service.getFoodAllCategoriesCodes(foodCode).right.get

        invalidateFoodRecord(foodCode, locale)
        invalidateProblemsForFood(foodCode, locale)

        codes.foreach(invalidateRecursiveCategoryProblems(_, locale))
    }
  }

  // Intentionally uncached
  def isFoodCodeAvailable(code: String): Either[LookupError, Boolean] = service.isFoodCodeAvailable(code)

  def createFood(newFood: NewFood): Either[DependentCreateError, Unit] = service.createFood(newFood).right.map {
    _ =>
      val locales = service.listLocales().right.get
      locales.keySet.foreach(invalidateUncategorisedFoods(_))
  }

  def createFoodWithTempCode(newFood: NewFood): Either[DependentCreateError, String] =
    service.createFoodWithTempCode(newFood).right.map {
      code =>
        val locales = service.listLocales().right.get
        locales.keySet.foreach(invalidateUncategorisedFoods(_))
        code
    }

  def deleteFood(foodCode: String): Either[DeleteError, Unit] = service.deleteFood(foodCode).right.map {
    _ =>
      val categories = service.getFoodAllCategoriesCodes(foodCode).right.get
      val locales = service.listLocales().right.get

      locales.keySet.foreach {
        locale_id =>

          invalidateFoodRecord(foodCode, locale_id)
          invalidateProblemsForFood(foodCode, locale_id)
          invalidateFoodParentCategories(foodCode, locale_id)

          if (categories.isEmpty)
            invalidateUncategorisedFoods(locale_id)

          categories.foreach {
            categoryCode =>
              invalidateProblemsForCategory(categoryCode, locale_id)
              invalidateCategoryContents(categoryCode, locale_id)
          }
      }
  }

  def updateCategoryMainRecord(categoryCode: String, categoryBase: MainCategoryRecord): Either[UpdateError, Unit] =
    service.updateCategoryMainRecord(categoryCode, categoryBase).right.map {
      _ =>
        val categories = service.getCategoryAllCategoriesCodes(categoryCode).right.get
        val locales = service.listLocales().right.get

        locales.keySet.foreach {
          locale_id =>
            invalidateCategoryRecord(categoryCode, locale_id)
            invalidateProblemsForCategory(categoryCode, locale_id)

            categories.foreach(invalidateRecursiveCategoryProblems(_, locale_id))
        }
    }

  def updateCategoryLocalRecord(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[LocalUpdateError, Unit] =
    service.updateCategoryLocalRecord(categoryCode, locale, categoryLocal).right.map {
      _ =>
        val categories = service.getCategoryAllCategoriesCodes(categoryCode).right.get

        invalidateCategoryRecord(categoryCode, locale)
        invalidateProblemsForCategory(categoryCode, locale)

        categories.foreach(invalidateRecursiveCategoryProblems(_, locale))
    }

  // Intentionally uncached
  def isCategoryCodeAvailable(code: String): Either[LookupError, Boolean] = service.isCategoryCodeAvailable(code)

  def createCategory(newCategory: NewCategory): Either[CreateError, Unit] = service.createCategory(newCategory) // root categories?

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit] = service.deleteCategory(categoryCode).right.map {
    _ =>
      val categories = getCategoryAllCategoriesCodes(categoryCode).right.get
      val locales = listLocales().right.get
      locales.keySet.foreach {
        locale_id =>
          invalidateCategoryRecord(categoryCode, locale_id)
          invalidateProblemsForCategory(categoryCode, locale_id)
          invalidateCategoryParentCategories(categoryCode, locale_id)
          invalidateRootCategories(locale_id)

          categories.foreach {
            categoryCode =>
              invalidateProblemsForCategory(categoryCode, locale_id)
              invalidateCategoryContents(categoryCode, locale_id)
          }
      }
  }

  def addFoodToCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit] = service.addFoodToCategory(categoryCode, foodCode).right.map {
    _ =>
      val locales = service.listLocales().right.get

      locales.keySet.foreach {
        locale_id =>
          invalidateFoodParentCategories(foodCode, locale_id)
          invalidateCategoryContents(categoryCode, locale_id)
          invalidateUncategorisedFoods(locale_id)

          invalidateProblemsForFood(foodCode, locale_id)
          invalidateProblemsForCategory(categoryCode, locale_id)
      }
  }

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit] =
    service.addSubcategoryToCategory(categoryCode, subcategoryCode).right.map {
      _ =>
        val locales = service.listLocales().right.get
        locales.keySet.foreach {
          locale_id =>
            invalidateRootCategories(locale_id)

            invalidateCategoryParentCategories(subcategoryCode, locale_id)
            invalidateCategoryContents(categoryCode, locale_id)

            invalidateProblemsForCategory(subcategoryCode, locale_id)
            invalidateProblemsForCategory(categoryCode, locale_id)
        }
    }

  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit] =
    service.removeFoodFromCategory(categoryCode, foodCode).right.map {
      _ =>
        val locales = service.listLocales().right.get
        locales.keySet.foreach {
          locale_id =>
            invalidateFoodAllCategories(foodCode, locale_id)
            invalidateFoodParentCategories(foodCode, locale_id)
            invalidateCategoryContents(categoryCode, locale_id)
            invalidateUncategorisedFoods(locale_id)

            invalidateProblemsForFood(foodCode, locale_id)
            invalidateProblemsForCategory(categoryCode, locale_id)
        }
    }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit] =
    service.removeSubcategoryFromCategory(categoryCode, subcategoryCode).right.map {
      _ =>
        val locales = service.listLocales().right.get
        locales.keySet.foreach {
          locale_id =>
            invalidateCategoryAllCategories(subcategoryCode, locale_id)
            invalidateCategoryParentCategories(subcategoryCode, locale_id)
            invalidateCategoryContents(categoryCode, locale_id)
            invalidateRootCategories(locale_id)

            invalidateProblemsForCategory(categoryCode, locale_id)
            invalidateProblemsForCategory(subcategoryCode, locale_id)
        }
    }

  def getAssociatedFoodsWithHeaders(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFoodWithHeader]] = cacheResult(associatedFoodsCacheKey(foodCode, locale)) {
    service.getAssociatedFoodsWithHeaders(foodCode, locale)
  }

  def updateAssociatedFoods(foodCode: String, locale: String, associatedFoods: Seq[AssociatedFood]): Either[LocalUpdateError, Unit] =
    service.updateAssociatedFoods(foodCode, locale, associatedFoods).right.map {
      _ =>
        invalidateAssociatedFoods(foodCode, locale)
    }

}