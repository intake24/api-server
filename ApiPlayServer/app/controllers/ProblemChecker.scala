package controllers

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
import uk.ac.ncl.openlab.intake24.services.AdminFoodDataService
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService
import uk.ac.ncl.openlab.intake24.services.FoodDataError
import uk.ac.ncl.openlab.intake24.services.LocaleManagementService

case class CategoryProblem(categoryCode: String, categoryName: String, problemCode: String)

case class FoodProblem(foodCode: String, foodName: String, problemCode: String)

case class RecursiveCategoryProblems(foodProblems: Seq[FoodProblem], categoryProblems: Seq[CategoryProblem]) {
  def count = foodProblems.size + categoryProblems.size
  def ++(other: RecursiveCategoryProblems) = RecursiveCategoryProblems(foodProblems ++ other.foodProblems, categoryProblems ++ other.categoryProblems)
}

class ProblemChecker @Inject() (userData: UserFoodDataService, adminData: AdminFoodDataService, locales: LocaleManagementService, deadbolt: DeadboltActions) extends Controller {

  val NutrientCodeMissing = "nutrient_code_missing"
  val NotAssignedToGroup = "not_assigned_to_group"
  val NotAssignedToCategory = "not_assigned_to_category"
  val PortionSizeMethodsEmpty = "no_portion_size_methods"
  val NoMethodDescOrImage = "no_method_desc_or_image"
  val LocalDescriptionMissing = "local_description_missing"
  val EmptyCategory = "empty_category"
  val SingleItem = "single_item_in_category"

  val maxReturnedProblems = 10

  def translationRequired(localeCode: String) = {
    val currentLocale = locales.get(localeCode).get

    currentLocale.prototypeLocale match {
      case Some(prototypeLocaleCode) => {
        val prototypeLocale = locales.get(prototypeLocaleCode).get

        currentLocale.respondentLanguage != prototypeLocale.respondentLanguage
      }
      case None => true
    }
  }

  def foodProblems(code: String, locale: String): Seq[FoodProblem] = {
    val foodDef = adminData.foodRecord(code, locale)
    val userFoodData = userData.foodData(code, locale)
    val uncatFoods = adminData.uncategorisedFoods(locale)

    val problems = Buffer[String]()

    (foodDef, userFoodData) match {
      case (Right(foodDef), Right((userFoodData, _))) => {
        if (userFoodData.nutrientTableCodes.isEmpty)
          problems += NutrientCodeMissing

        if (userFoodData.groupCode == 0)
          problems += NotAssignedToGroup

        if (uncatFoods.contains(code))
          problems += NotAssignedToCategory

        if (userFoodData.portionSize.isEmpty)
          problems += PortionSizeMethodsEmpty

        if (userFoodData.portionSize.size > 1 && userFoodData.portionSize.exists(x => x.description == "no description" || x.imageUrl == "images/placeholder.jpg"))
          problems += NoMethodDescOrImage

        if (foodDef.local.localDescription.isEmpty && !foodDef.local.doNotUse && translationRequired(locale))
          problems += LocalDescriptionMissing

        problems.toSeq.map(pcode => FoodProblem(code, userFoodData.localDescription, pcode))
      }
      case _ => Seq()
    }

  }

  def categoryProblems(code: String, locale: String): Seq[CategoryProblem] = {
    val contents = adminData.categoryContents(code, locale)

    val size = contents.foods.size + contents.subcategories.size

    val problems = Buffer[String]()

    if (size == 0)
      problems += EmptyCategory

    if (size == 1)
      problems += SingleItem

    adminData.categoryRecord(code, locale) match {

      case Right(record) => {
        if (record.local.localDescription.isEmpty && translationRequired(locale))
          problems += LocalDescriptionMissing
        problems.toSeq.map(pcode => CategoryProblem(code, record.local.localDescription.getOrElse(record.main.englishDescription), pcode))
      }
      // FIXME: Return Either instead
      case _ => Seq()
    }

  }

  def recursiveCategoryProblems(code: String, locale: String, maxProblems: Int): RecursiveCategoryProblems = {

    def collectSubcategoryProblems(rem: Seq[CategoryHeader], problems: RecursiveCategoryProblems, slots: Int): RecursiveCategoryProblems = {
      if (rem.isEmpty || slots <= 0)
        problems
      else {
        val p = recursiveCategoryProblems(rem.head.code, locale, slots)
        collectSubcategoryProblems(rem.tail, problems ++ p, slots - p.count)
      }
    }

    if (maxProblems <= 0)
      RecursiveCategoryProblems(Seq(), Seq())
    else {
      val contents = adminData.categoryContents(code, locale)

      var remainingProblemSlots = maxProblems

      val ownProblems = categoryProblems(code, locale).take(remainingProblemSlots)

      remainingProblemSlots -= ownProblems.size

      val fdProblems = contents.foods.sortBy(fh => fh.localDescription.getOrElse(fh.englishDescription)).flatMap(fh => foodProblems(fh.code, locale)).take(remainingProblemSlots)

      remainingProblemSlots -= fdProblems.length

      val subcatProblems =
        if (remainingProblemSlots > 0)
          contents.subcategories.sortBy(ch => ch.localDescription.getOrElse(ch.englishDescription)).flatMap(ch => categoryProblems(ch.code, locale)).take(remainingProblemSlots)
        else
          Seq()

      remainingProblemSlots -= subcatProblems.length

      collectSubcategoryProblems(contents.subcategories, RecursiveCategoryProblems(fdProblems, ownProblems ++ subcatProblems), remainingProblemSlots)
    }
  }

  def checkFood(code: String, locale: String) = deadbolt.Restrict(List(Array("superuser"))) {
    Action {
      Ok(write(foodProblems(code, locale))).as(ContentTypes.JSON)
    }
  }

  def checkCategory(code: String, locale: String) = deadbolt.Restrict(List(Array("superuser"))) {
    Action {

      Ok(write(categoryProblems(code, locale))).as(ContentTypes.JSON)
    }
  }

  def checkCategoryRecursive(code: String, locale: String) = deadbolt.Restrict(List(Array("superuser"))) {
    Action {

      Ok(write(recursiveCategoryProblems(code, locale, maxReturnedProblems))).as(ContentTypes.JSON)
    }
  }
}
