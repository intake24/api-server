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

class AdminFoodData @Inject() (service: AdminFoodDataService, deadbolt: DeadboltActions) extends Controller {

  def rootCategories(locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.rootCategories(locale))).as(ContentTypes.JSON)
    }
  }

  def uncategorisedFoods(locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.uncategorisedFoods(locale))).as(ContentTypes.JSON)
    }
  }

  def categoryContents(code: String, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.categoryContents(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodParentCategories(code: String, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.foodParentCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodAllCategories(code: String, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.foodAllCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodDef(code: String, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.foodDef(code, locale))).as(ContentTypes.JSON)
    }
  }

  def categoryDef(code: String, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.categoryDef(code, locale))).as(ContentTypes.JSON)
    }
  }

  def categoryParentCategories(code: String, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.categoryParentCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def categoryAllCategories(code: String, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.categoryAllCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def allAsServedSets() = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.allAsServedSets())).as(ContentTypes.JSON)
    }
  }

  def allDrinkware = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.allDrinkware())).as(ContentTypes.JSON)
    }
  }

  def allGuideImages() = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.allGuideImages())).as(ContentTypes.JSON)
    }
  }

  def allFoodGroups(locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.allFoodGroups(locale))).as(ContentTypes.JSON)
    }
  }

  def foodGroup(id: Int, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      service.foodGroup(id, locale) match {
        case Some(group) => Ok(write(group)).as(ContentTypes.JSON)
        case None => BadRequest
      }
    }
  }

  def searchFoods(searchTerm: String, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.searchFoods(searchTerm, locale))).as(ContentTypes.JSON)
    }
  }

  def searchCategories(searchTerm: String, locale: String) = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.searchCategories(searchTerm, locale))).as(ContentTypes.JSON)
    }
  }

  def nutrientTables() = deadbolt.Pattern("api.readAdminFoodData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.nutrientTables())).as(ContentTypes.JSON)
    }
  }
}
