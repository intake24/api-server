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
import be.objectify.deadbolt.core.PatternType
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

case class CachedProblemChecker @Inject() (userData: FoodDatabaseUserImpl, adminData: FoodDatabaseAdminImpl, cache: CacheApi)
    extends ProblemCheckerService with ProblemCheckerCache with Timing {

  val log = LoggerFactory.getLogger(classOf[CachedProblemChecker])

  val NutrientCodeMissing = "nutrient_code_missing"
  val NotAssignedToGroup = "not_assigned_to_group"
  val NotAssignedToCategory = "not_assigned_to_category"
  val PortionSizeMethodsEmpty = "no_portion_size_methods"
  val NoMethodDescOrImage = "no_method_desc_or_image"
  val LocalDescriptionMissing = "local_description_missing"
  val EmptyCategory = "empty_category"
  val SingleItem = "single_item_in_category"

  val maxReturnedProblems = 10

  private def isTranslationRequired(localeCode: String): Boolean = cache.getOrElse(translationRequiredCacheKey(localeCode)) {
    val currentLocale = adminData.getLocale(localeCode).right.get

    currentLocale.prototypeLocale match {
      case Some(prototypeLocaleCode) => {
        val prototypeLocale = adminData.getLocale(prototypeLocaleCode).right.get

        currentLocale.respondentLanguage != prototypeLocale.respondentLanguage
      }
      case None => {
        true
      }
    }
  }

  def getFoodProblems(code: String, locale: String): Either[LocalLookupError, Seq[FoodProblem]] = cache.getOrElse(foodProblemsCacheKey(code, locale)) {

    for (
      adminFoodRecord <- adminData.getFoodRecord(code, locale).right;
      userFoodRecord <- userData.foodData(code, locale).right.map(_._1).right;
      uncategorisedFoods <- adminData.getUncategorisedFoods(locale).right
    ) yield {
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

      if (adminFoodRecord.local.localDescription.isEmpty && !adminFoodRecord.local.doNotUse && isTranslationRequired(locale))
        problems += LocalDescriptionMissing

      problems.toSeq.map(pcode => FoodProblem(code, userFoodRecord.localDescription, pcode))

    }

  }

  def getCategoryProblems(code: String, locale: String): Either[LocalLookupError, Seq[CategoryProblem]] = cache.getOrElse(categoryProblemsCacheKey(code, locale)) {
    for (
      contents <- adminData.getCategoryContents(code, locale).right;
      record <- adminData.getCategoryRecord(code, locale).right
    ) yield {
      val size = contents.foods.size + contents.subcategories.size

      val problems = Buffer[String]()

      if (size == 0)
        problems += EmptyCategory

      if (size == 1)
        problems += SingleItem

      if (record.local.localDescription.isEmpty && isTranslationRequired(locale))
        problems += LocalDescriptionMissing
      problems.toSeq.map(pcode => CategoryProblem(code, record.local.localDescription.getOrElse(record.main.englishDescription), pcode))
    }
  }

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

  def recursiveCategoryProblems(code: String, locale: String, maxProblems: Int): Either[LocalLookupError, RecursiveCategoryProblems] =
    cache.getOrElse(recursiveCategoryProblemsCacheKey(code, locale)) {

      def collectSubcategoryProblems(rem: Seq[CategoryHeader], problems: Either[LocalLookupError, RecursiveCategoryProblems], slots: Int): Either[LocalLookupError, RecursiveCategoryProblems] = {
        if (rem.isEmpty || slots <= 0)
          problems
        else {
          recursiveCategoryProblems(rem.head.code, locale, slots).right.flatMap {
            p =>
              collectSubcategoryProblems(rem.tail, problems.right.map(_ ++ p), slots - p.count)
          }
        }
      }

      if (maxProblems <= 0)
        Right(RecursiveCategoryProblems(Seq(), Seq()))
      else {
        for (
          contents <- adminData.getCategoryContents(code, locale).right;
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
}
