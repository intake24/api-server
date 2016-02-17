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
import play.api.http.ContentTypes
import play.api.mvc.Action
import play.api.mvc.Controller
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService
import upickle.default._

class UserDataService @Inject() (service: UserFoodDataService, deadbolt: DeadboltActions) extends Controller {

  def categoryContents(code: String, locale: String) = deadbolt.Pattern("api.readUserFoodsData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.categoryContents(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodData(code: String, locale: String) = deadbolt.Pattern("api.readUserFoodsData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.foodData(code, locale))).as(ContentTypes.JSON)
    }
  }

  def brandNames(code: String, locale: String) = deadbolt.Pattern("api.readUserFoodsData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.brandNames(code, locale))).as(ContentTypes.JSON)
    }
  }

  def asServedDef(id: String) = deadbolt.Pattern("api.readUserFoodsData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.asServedDef(id))).as(ContentTypes.JSON)
    }
  }

  def drinkwareDef(id: String) = deadbolt.Pattern("api.readUserFoodsData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.drinkwareDef(id))).as(ContentTypes.JSON)
    }
  }

  def guideDef(id: String) = deadbolt.Pattern("api.readUserFoodsData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.guideDef(id))).as(ContentTypes.JSON)
    }
  }

  def associatedFoodPrompts(code: String, locale: String) = deadbolt.Pattern("api.readUserFoodsData", PatternType.EQUALITY) {
    Action {
      Ok(write(service.associatedFoodPrompts(code, locale))).as(ContentTypes.JSON)
    }
  }
}
