package cache

import play.api.mvc.Controller
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.ac.ncl.openlab.intake24.nutrients.EnergyKcal
import play.api.libs.json.JsError
import scala.concurrent.Future
import upickle.default._
import com.oracle.webservices.internal.api.message.ContentType
import play.api.http.ContentTypes
import javax.inject.Inject
import be.objectify.deadbolt.scala.DeadboltActions

import scala.collection.mutable.Buffer
import uk.ac.ncl.openlab.intake24.CategoryHeader
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.services.util.Timing
import play.api.cache.CacheApi
import models.FoodProblem
import models.CategoryProblem
import models.RecursiveCategoryProblems
import modules.ProblemCheckerService
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodDatabaseUserImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDataService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodBrowsingAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.MainCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.LocalCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.NewLocalCategoryRecord
import uk.ac.ncl.openlab.intake24.NewMainCategoryRecord

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

  def sequence[E, T](s: Seq[Either[E, T]]): Either[E, Seq[T]] = {
    val z: Either[E, Seq[T]] = Right(Seq())
    s.foldLeft(z) {
      (result, next) =>
        for (
          ts <- result.right;
          t <- next.right
        ) yield (t +: ts)
    }.right.map(_.reverse)
  }

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

        if (userFoodRecord.portionSize.isEmpty)
          problems += PortionSizeMethodsEmpty

        if (userFoodRecord.portionSize.size > 1 && userFoodRecord.portionSize.exists(x => x.description == "no description" || x.imageUrl == "images/placeholder.jpg"))
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

  def invalidateLocalFoodProblems(code: String, locale: String) = adminBrowsing.getFoodAllCategoriesCodes(code).right.map {
    superCategories =>
      removeCached(foodProblemsCacheKey(code, locale))
      superCategories.foreach {
        categoryCode =>
          invalidateChildProblems(categoryCode, locale)
      }
  }

  def invalidateFoodProblems(code: String): Either[DatabaseError, Unit] = locales.listLocales().right.map {
    locales =>
      locales.keySet.foreach {
        locale => invalidateLocalFoodProblems(code, locale)
      }
  }

  def invalidateLocalCategoryProblems(code: String, locale: String) = adminBrowsing.getCategoryAllCategoriesCodes(code).right.map {
    superCategories =>
      removeCached(categoryProblemsCacheKey(code, locale))
      superCategories.foreach {
        categoryCode =>
          invalidateChildProblems(categoryCode, locale)
      }
  }

  def invalidateCategoryProblems(code: String) = locales.listLocales().right.map {
    locales =>
      locales.keySet.foreach {
        locale => invalidateLocalCategoryProblems(code, locale)
      }
  }

  def invalidateChildProblems(code: String, locale: String) = removeCached(recursiveCategoryProblemsCacheKey(code, locale))

  def onMainCategoryRecordUpdated(code: String, update: MainCategoryRecordUpdate) = {
    invalidateCategoryProblems(code)
    update.parentCategories.foreach {
      parent =>
        invalidateCategoryProblems(parent)
    }
  }

  def onLocalCategoryRecordUpdated(code: String, update: LocalCategoryRecordUpdate, locale: String) = invalidateLocalCategoryProblems(code, locale)

  // FIXME: Actually need to handle "about to be deleted" to invalidate parents properly
  def onCategoryDeleted(code: String) = invalidateCategoryProblems(code)

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
    record.parentCategories.foreach {
      code =>
        invalidateCategoryProblems(code)
    }
  }

  def onLocalCategoryRecordCreated(code: String, record: NewLocalCategoryRecord, locale: String) = {
    invalidateLocalCategoryProblems(code, locale)
  }

  def onAllFoodsDeleted() = {
    removeAllCachedResults()
  }

  def onFoodCreated(code: String) = {
    invalidateFoodProblems(code)
  }

  def onFoodDeleted(code: String) = {
    invalidateFoodProblems(code)
  }

  def onLocalFoodRecordCreated(code: String, locale: String) = {
    invalidateLocalFoodProblems(code, locale)
  }

  def onLocalFoodRecordUpdated(code: String, locale: String) = {
    invalidateLocalFoodProblems(code, locale)
  }

  def onMainFoodRecordUpdated(code: String) = {
    invalidateFoodProblems(code)
  }

  def onLocaleCreated(id: String) = {}

  def onLocaleUpdated(id: String) = {}
}
