package cache

import javax.inject.Inject

import models.{CategoryProblem, FoodProblem, RecursiveCategoryProblems}
import modules.ProblemCheckerService
import org.slf4j.LoggerFactory
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24._
import uk.ac.ncl.openlab.intake24.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodBrowsingAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDataService
import uk.ac.ncl.openlab.intake24.services.util.Timing

import scala.collection.mutable.Buffer

case class CachedProblemChecker @Inject() (
  categories: ObservableCategoriesAdminService,
  foods: ObservableFoodsAdminService,
  locales: ObservableLocalesAdminService,
  userFoods: FoodDataService,
  adminBrowsing: FoodBrowsingAdminService,
  cache: CacheApi)
    extends ProblemCheckerService
    with Timing
    with CacheResult
    with CategoriesAdminObserver
    with FoodsAdminObserver
    with LocalesAdminObserver {

  import uk.ac.ncl.openlab.intake24.errors.ErrorUtils._

  val logger = LoggerFactory.getLogger(classOf[CachedProblemChecker])

  var knownCacheKeys = Set[String]()

  categories.addObserver(this)
  foods.addObserver(this)
  locales.addObserver(this)

  val NutrientCodeMissing = "nutrient_code_missing"
  val NotAssignedToGroup = "not_assigned_to_group"
  val NotAssignedToCategory = "not_assigned_to_category"
  val PortionSizeMethodsEmpty = "no_portion_size_methods"
  val NoMethodDescOrImage = "no_method_desc_or_image"
  val LocalDescriptionMissing = "local_description_missing"
  val EmptyCategory = "empty_category"
  val SingleItem = "single_item_in_category"

  def foodProblemsCacheKey(code: String, locale: String) = s"CachedProblemChecker.foodProblems.$locale.$code"
  def categoryProblemsCacheKey(code: String, locale: String) = s"CachedProblemChecker.categoryProblems.$locale.$code"
  def recursiveCategoryProblemsCacheKey(code: String, locale: String) = s"CachedProblemChecker.recursiveCategoryProblems.$locale.$code"


  val maxReturnedProblems = 10

  def getFoodProblems(code: String, locale: String): Either[LocalLookupError, Seq[FoodProblem]] = cachePositiveResult(foodProblemsCacheKey(code, locale)) {
    for (
      adminFoodRecord <- foods.getFoodRecord(code, locale).right;
      userFoodRecord <- userFoods.getFoodData(code, locale).right.map(_._1).right;
      uncategorisedFoods <- adminBrowsing.getUncategorisedFoods(locale).right;
      translationRequired <- locales.isTranslationRequired(locale).right
    ) yield {

      if (!adminFoodRecord.allowedInLocale(locale))
        Seq()
      else {
        val problems = Buffer[String]()

        if (userFoodRecord.nutrientTableCodes.isEmpty)
          problems += NutrientCodeMissing

        if (userFoodRecord.groupCode == 0)
          problems += NotAssignedToGroup

        if (uncategorisedFoods.exists(_.code == code))
          problems += NotAssignedToCategory

        if (userFoodRecord.portionSizeMethods.isEmpty)
          problems += PortionSizeMethodsEmpty

        if (userFoodRecord.portionSizeMethods.size > 1 && userFoodRecord.portionSizeMethods.exists(x => x.description == "no description" || x.imageUrl == "images/placeholder.jpg"))
          problems += NoMethodDescOrImage

        if (adminFoodRecord.local.localDescription.isEmpty && !adminFoodRecord.local.doNotUse && translationRequired)
          problems += LocalDescriptionMissing

        problems.toSeq.map(pcode => FoodProblem(code, userFoodRecord.localDescription, pcode))
      }
    }
  }

  def getCategoryProblems(code: String, locale: String): Either[LocalLookupError, Seq[CategoryProblem]] = cachePositiveResult(categoryProblemsCacheKey(code, locale)) {
    for (
      contents <- adminBrowsing.getCategoryContents(code, locale).right;
      record <- categories.getCategoryRecord(code, locale).right;
      translationRequired <- locales.isTranslationRequired(locale).right
    ) yield {
      val size = contents.foods.size + contents.subcategories.size

      val problems = Buffer[String]()

      if (size == 0)
        problems += EmptyCategory

      if (size == 1)
        problems += SingleItem

      if (record.local.localDescription.isEmpty && translationRequired)
        problems += LocalDescriptionMissing
      problems.toSeq.map(pcode => CategoryProblem(code, record.local.localDescription.getOrElse(record.main.englishDescription), pcode))
    }
  }

  def getRecursiveCategoryProblems(code: String, locale: String, maxProblems: Int): Either[LocalLookupError, RecursiveCategoryProblems] =
    cachePositiveResult(recursiveCategoryProblemsCacheKey(code, locale)) {

      def collectSubcategoryProblems(rem: Seq[CategoryHeader], problems: Either[LocalLookupError, RecursiveCategoryProblems], slots: Int): Either[LocalLookupError, RecursiveCategoryProblems] = {
        if (rem.isEmpty || slots <= 0)
          problems
        else {
          getRecursiveCategoryProblems(rem.head.code, locale, slots).right.flatMap {
            p =>
              collectSubcategoryProblems(rem.tail, problems.right.map(_ ++ p), slots - p.count)
          }
        }
      }

      if (maxProblems <= 0)
        Right(RecursiveCategoryProblems(Seq(), Seq()))
      else {
        for (
          contents <- adminBrowsing.getCategoryContents(code, locale).right;
          ownProblems <- getCategoryProblems(code, locale).right;
          foodProblems <- sequence(contents.foods.map(h => getFoodProblems(h.code, locale))).right.map(_.flatten).right;
          subcategoryProblems <- sequence(contents.subcategories.map(h => getCategoryProblems(h.code, locale))).right.map(_.flatten).right;
          result <- {
            var remainingProblemSlots = maxProblems

            val truncatedOwnProblems = ownProblems.take(remainingProblemSlots)

            remainingProblemSlots = Math.max(0, remainingProblemSlots - ownProblems.size)

            val truncatedFoodProblems = foodProblems.take(remainingProblemSlots)

            remainingProblemSlots = Math.max(0, remainingProblemSlots - foodProblems.size)

            val truncatedSubcategoryProblems = subcategoryProblems.take(remainingProblemSlots)

            remainingProblemSlots = Math.max(0, remainingProblemSlots - subcategoryProblems.size)

            collectSubcategoryProblems(contents.subcategories, Right(RecursiveCategoryProblems(truncatedFoodProblems, truncatedOwnProblems ++ truncatedSubcategoryProblems)), remainingProblemSlots)

          }.right
        ) yield result
      }
    }

  def invalidateFood(code: String): Unit = {
    val allLocales = locales.listLocales().right.get.keySet
    val parentCodes = adminBrowsing.getFoodAllCategoriesCodes(code).right.get

    for (locale <- allLocales) yield {
      removeCached(foodProblemsCacheKey(code, locale))

      for (categoryCode <- parentCodes) yield {
        removeCached(categoryProblemsCacheKey(categoryCode, locale))
        removeCached(recursiveCategoryProblemsCacheKey(categoryCode, locale))
      }
    }
  }

  def invalidateCategory(code: String): Unit = {
    val allLocales = locales.listLocales().right.get.keySet

    // need to invalidate all locales because of potential locale inheritance

    for (locale <- allLocales) yield {
      removeCached(categoryProblemsCacheKey(code, locale))
      removeCached(recursiveCategoryProblemsCacheKey(code, locale))

      val parentCodes = adminBrowsing.getCategoryAllCategoriesCodes(code).right.get

      for (
        parentCode <- parentCodes
      ) yield {
        removeCached(categoryProblemsCacheKey(parentCode, locale))
        removeCached(recursiveCategoryProblemsCacheKey(parentCode, locale))
      }

      val descendants = adminBrowsing.getAllCategoryDescendantsCodes(code).right.get

      for (
        categoryCode <- descendants.subcategories
      ) yield {
        removeCached(categoryProblemsCacheKey(categoryCode, locale))
        removeCached(recursiveCategoryProblemsCacheKey(categoryCode, locale))
      }

      for (
        foodCode <- descendants.foods
      ) yield {
        removeCached(foodProblemsCacheKey(foodCode, locale))
      }
    }
  }

  def onMainCategoryRecordUpdated(code: String, update: MainCategoryRecordUpdate) = {
    invalidateCategory(code)
  }

  def onLocalCategoryRecordUpdated(code: String, update: LocalCategoryRecordUpdate, locale: String) = {
    invalidateCategory(code)
  }

  //FIXME: Race conditions! This needs to be done AFTER the food has been deleted, but the parents 
  // can only be read before it is deleted which can result in data being re-cached right before it 
  // is actually deleted. Low probability, so ignoring for now.
  def onCategoryToBeDeleted(code: String) = {
    invalidateCategory(code)
  }

  def onCategoryDeleted(code: String) = { }

  def onLocaleDeleted(id: String) = {
    removeCachedByPredicate {
      k =>
        k.startsWith(foodProblemsCacheKey("", id)) ||
        k.startsWith(categoryProblemsCacheKey("", id)) ||
        k.startsWith(recursiveCategoryProblemsCacheKey("", id))
    }
  }

  def onAllCategoriesDeleted() = {
    removeAllCachedResults()
  }

  def onMainCategoryRecordCreated(record: NewMainCategoryRecord) = {
    invalidateCategory(record.code)
  }

  def onLocalCategoryRecordCreated(code: String, record: NewLocalCategoryRecord, locale: String) = {
    invalidateCategory(code)
  }

  def onAllFoodsDeleted() = {
    removeAllCachedResults()
  }

  def onFoodCreated(code: String) = {
    invalidateFood(code)
  }

  //FIXME: Race conditions! This needs to be done AFTER the food has been deleted, but the parents 
  // can only be read before it is deleted which can result in data being re-cached right before it 
  // is actually deleted. Low probability, so ignoring for now.
  def onFoodToBeDeleted(code: String) = {
    invalidateFood(code)
  }

  def onFoodDeleted(code: String) = { }

  def onLocalFoodRecordCreated(code: String, locale: String) = {
    invalidateFood(code)
  }

  def onLocalFoodRecordUpdated(code: String, locale: String) = {
    invalidateFood(code)
  }

  def onMainFoodRecordUpdated(code: String) = {
    invalidateFood(code)
  }

  def onLocaleCreated(id: String) = {}

  def onLocaleUpdated(id: String) = {}
}
