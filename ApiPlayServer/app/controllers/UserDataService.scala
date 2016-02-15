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
import uk.ac.ncl.openlab.intake24.services.AdminFoodDataService

class FoodDataRead @Inject() (foodDataService: AdminFoodDataService, deadbolt: DeadboltActions) extends Controller {
  
  def allCategories(locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.allCategories(locale))).as(ContentTypes.JSON)      
    }
  }

  def rootCategories(locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.rootCategories(locale))).as(ContentTypes.JSON)
    }
  }

  def uncategorisedFoods(locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.uncategorisedFoods(locale))).as(ContentTypes.JSON)
    }
  }

  def categoryContents(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.categoryContents(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodData(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.foodData(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodParentCategories(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.foodParentCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodAllCategories(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.foodAllCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodDef(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.foodDef(code, locale))).as(ContentTypes.JSON)
    }
  }

  def categoryDef(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.categoryDef(code, locale))).as(ContentTypes.JSON)
    }
  }

  def categoryParentCategories(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.categoryParentCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def categoryAllCategories(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.categoryAllCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def brandNames(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.brandNames(code, locale))).as(ContentTypes.JSON)
    }
  }

  def allAsServedSets() = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.allAsServedSets())).as(ContentTypes.JSON)
    }
  }

  def asServedDef(id: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.asServedDef(id))).as(ContentTypes.JSON)
    }
  }

  def allDrinkware = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.allDrinkware())).as(ContentTypes.JSON)
    }
  }

  def drinkwareDef(id: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.drinkwareDef(id))).as(ContentTypes.JSON)
    }
  }

  def allGuideImages() = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.allGuideImages())).as(ContentTypes.JSON)
    }
  }

  def guideDef(id: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.guideDef(id))).as(ContentTypes.JSON)
    }
  }

  def associatedFoodPrompts(code: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.associatedFoodPrompts(code, locale))).as(ContentTypes.JSON)
    }
  }

  def allFoodGroups(locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.allFoodGroups(locale))).as(ContentTypes.JSON)
    }
  }

  def foodGroup(id: Int, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      foodDataService.foodGroup(id, locale) match {
        case Some(group) => Ok(write(group)).as(ContentTypes.JSON)
        case None => BadRequest
      }
    }
  }

  def splitList(locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.splitList(locale))).as(ContentTypes.JSON)
    }
  }

  def synsets(locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.synsets(locale))).as(ContentTypes.JSON)
    }
  }

  def searchFoods(searchTerm: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.searchFoods(searchTerm, locale))).as(ContentTypes.JSON)
    }
  }

  def searchCategories(searchTerm: String, locale: String) = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.searchCategories(searchTerm, locale))).as(ContentTypes.JSON)
    }
  }
  
  def nutrientTables() = deadbolt.Pattern("api.foods.read", PatternType.EQUALITY) {
    Action {
      Ok(write(foodDataService.nutrientTables())).as(ContentTypes.JSON)
    }
  }

} 