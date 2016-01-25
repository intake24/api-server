package controllers

import play.api.mvc.Controller
import play.api.libs.json.Json
import play.api.mvc.Action
import net.scran24.fooddef.nutrients.EnergyKcal
import play.api.libs.json.JsError
import scala.concurrent.Future
import upickle.default._
import com.oracle.webservices.internal.api.message.ContentType
import play.api.http.ContentTypes
import javax.inject.Inject
import be.objectify.deadbolt.scala.DeadboltActions
import be.objectify.deadbolt.core.PatternType
import uk.ac.ncl.openlab.intake24.services.FoodDataService
import scala.collection.mutable.Buffer

case class CategoryProblem(categoryCode: String, problemCode: String)

case class FoodProblem(foodCode: String, problemCode: String)

case class RecursiveCategoryProblems(foodProblems: Map[String, Seq[FoodProblem]], subcategoryProblems: Map[String, Seq[CategoryProblem]])

class ProblemChecker @Inject() (foodDataService: FoodDataService, deadbolt: DeadboltActions) extends Controller {

  val NutrientCodeMissing = "nutrient_code_missing"
  val NotAssignedToGroup = "not_assigned_to_group"
  val NotAssignedToCategory = "not_assigned_to_category"
  val PortionSizeMethodsEmpty = "no_portion_size_methods"
  val NoMethodDescOrImage = "no_method_desc_or_image"
  val LocalDataEmpty = "no_local_data"
  val EmptyCategory = "empty_category"
  val SingleItem = "single_item_in_category"

  def foodProblems(locale: String, code: String): Seq[FoodProblem] = {
    val foodDef = foodDataService.foodDef(code, locale)
    val foodData = foodDataService.foodData(code, locale)
    val uncatFoods = foodDataService.uncategorisedFoods(locale)

    val problems = Buffer[String]()

    if (foodData.nutrientTableCodes.isEmpty)
      problems += NutrientCodeMissing

    if (foodData.groupCode == 0)
      problems += NotAssignedToGroup

    if (uncatFoods.contains(code))
      problems += NotAssignedToCategory

    if (foodData.portionSize.isEmpty)
      problems += PortionSizeMethodsEmpty

    if (foodData.portionSize.size > 1 && foodData.portionSize.exists(x => x.description == "no description" || x.imageUrl == "images/placeholder.jpg"))
      problems += NoMethodDescOrImage

    if (foodDef.localData.version.isEmpty)
      problems += LocalDataEmpty

    problems.toSeq.map(pcode => FoodProblem(code, pcode))
  }

  def categoryProblems(locale: String, code: String): Seq[CategoryProblem] = {
    val contents = foodDataService.categoryContents(code, locale)

    val size = contents.foods.size + contents.subcategories.size

    val problems = Buffer[String]()

    if (size == 0)
      problems += EmptyCategory

    if (size == 1)
      problems += SingleItem

    if (foodDataService.categoryDef(code, locale).localData.version.isEmpty)
      problems += LocalDataEmpty

    problems.toSeq.map(pcode => CategoryProblem(code, pcode))
  }

  def recursiveCategoryProblems(locale: String, code: String): RecursiveCategoryProblems = {
    def allFoods(code: String): Seq[String] = {
      val contents = foodDataService.categoryContents(code, locale)
      contents.foods.map(_.code) ++ contents.subcategories.flatMap(cat => allFoods(cat.code))
    }

    def allSubcategories(code: String): Seq[String] = {
      val contents = foodDataService.categoryContents(code, locale)
      contents.subcategories.map(_.code) ++ foodDataService.categoryContents(code, locale).subcategories.flatMap(cat => allSubcategories(cat.code))
    }

    val subcategoryProblems = allSubcategories(code).map(c => (c, categoryProblems(c, locale))).toMap 

    val foodProblems_ = allFoods(code).map(c => (c, foodProblems(c, locale))).toMap 
      
    
    RecursiveCategoryProblems(foodProblems_, subcategoryProblems)
  }

  def checkFood(locale: String, code: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodProblems(locale, code))).as(ContentTypes.JSON)
    }
  }

  def checkCategory(locale: String, code: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {

      Ok(write(categoryProblems(locale, code))).as(ContentTypes.JSON)
    }
  }

  def checkCategoryRecursive(locale: String, code: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {

      Ok(write(recursiveCategoryProblems(code, locale))).as(ContentTypes.JSON)
    }
  }

}
