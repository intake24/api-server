/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package controllers

import be.objectify.deadbolt.core.PatternType
import be.objectify.deadbolt.scala.DeadboltActions
import javax.inject.Inject
import net.scran24.fooddef.CategoryBase
import net.scran24.fooddef.CategoryLocal
import net.scran24.fooddef.FoodBase
import net.scran24.fooddef.FoodLocal
import uk.ac.ncl.openlab.intake24.services.FoodDataEditingService
import uk.ac.ncl.openlab.intake24.services.InvalidRequest
import uk.ac.ncl.openlab.intake24.services.SqlException
import uk.ac.ncl.openlab.intake24.services.Success
import uk.ac.ncl.openlab.intake24.services.UpdateResult
import uk.ac.ncl.openlab.intake24.services.VersionConflict
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result
import upickle.Invalid
import upickle.default._
import net.scran24.fooddef.InheritableAttributes
import uk.ac.ncl.openlab.intake24.services.NewFood
import uk.ac.ncl.openlab.intake24.services.NewCategory
import play.api.libs.json.JsValue
import play.api.libs.json.JsBoolean

class FoodDataWrite @Inject() (foodEditingService: FoodDataEditingService, deadbolt: DeadboltActions) extends Controller {

  def translateUpdateResult(result: UpdateResult): Result = result match {
    case Success => Ok
    case VersionConflict => Conflict
    case InvalidRequest(errorCode, message) => BadRequest(Json.obj("error" -> "invalid_request", "code" -> errorCode, "message" -> message))
    case SqlException(message) => InternalServerError(Json.obj("error" -> "sql_exception", "message" -> message))
  }

  def tryWithPickle(block: => Result) =
    try {
      block
    } catch {
      case Invalid.Data(_, msg) => BadRequest(Json.obj("error" -> "json_exception", "message" -> msg))
      case Invalid.Json(msg, input) => BadRequest(Json.obj("error" -> "json_exception", "message" -> msg))
    }

  def updateFoodBase(foodCode: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(foodEditingService.updateFoodBase(foodCode, read[FoodBase](request.body)))
      }
    }
  }

  def updateFoodLocal(foodCode: String, locale: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(foodEditingService.updateFoodLocal(foodCode, locale, read[FoodLocal](request.body)))
      }
    }
  }
  
  def isFoodCodeAvailable(code: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action {
      Ok(JsBoolean(foodEditingService.isFoodCodeAvailable(code)))
    }
  }

  def createFood() = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(foodEditingService.createFood(read[NewFood](request.body)))
      }
    }
  }

  def deleteFood(code: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action {
      translateUpdateResult(foodEditingService.deleteFood(code))
    }
  }

  def addFoodToCategory(categoryCode: String, foodCode: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action {
      translateUpdateResult(foodEditingService.addFoodToCategory(categoryCode, foodCode))

    }
  }
  def removeFoodFromCategory(categoryCode: String, foodCode: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action {
      translateUpdateResult(foodEditingService.removeFoodFromCategory(categoryCode, foodCode))
    }
  }

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action {
      translateUpdateResult(foodEditingService.addSubcategoryToCategory(categoryCode, subcategoryCode))
    }
  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action {
      translateUpdateResult(foodEditingService.removeSubcategoryFromCategory(categoryCode, subcategoryCode))

    }
  }

  def isCategoryCodeAvailable(code: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action {
      Ok(JsBoolean(foodEditingService.isCategoryCodeAvailable(code)))
    }
  }

  def createCategory() = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(foodEditingService.createCategory(read[NewCategory](request.body)))
      }
    }
  }

  def updateCategoryBase(categoryCode: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(foodEditingService.updateCategoryBase(categoryCode, read[CategoryBase](request.body)))
      }
    }
  }

  def updateCategoryLocal(categoryCode: String, locale: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(foodEditingService.updateCategoryLocal(categoryCode, locale, read[CategoryLocal](request.body)))
      }
    }
  }

  def deleteCategory(categoryCode: String) = deadbolt.Pattern("api.foods.write", PatternType.EQUALITY) {
    Action {
      translateUpdateResult(foodEditingService.deleteCategory(categoryCode))
    }
  }
}