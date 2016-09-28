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

import scala.concurrent.Future

import be.objectify.deadbolt.scala.DeadboltActions
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller

import security.DeadboltActionsAdapter
import security.Roles
import uk.ac.ncl.openlab.intake24.LocalFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.MainFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.NewMainFoodRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import upickle.default._

class FoodsAdminController @Inject() (service: FoodsAdminService, deadbolt: DeadboltActionsAdapter) extends Controller
    with PickleErrorHandler
    with ApiErrorHandler {

  def getFoodRecord(code: String, locale: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.getFoodRecord(code, locale))
    }
  }

  def isFoodCodeAvailable(code: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.isFoodCodeAvailable(code))
    }
  }

  def isFoodCode(code: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.isFoodCode(code))
    }
  }

  def createFood() = deadbolt.restrict(Roles.superuser)(parse.tolerantText) {
    request =>
      Future {
        tryWithPickle {
          translateError(service.createFood(read[NewMainFoodRecord](request.body)))
        }
      }
  }

  def createFoodWithTempCode() = deadbolt.restrict(Roles.superuser)(parse.tolerantText) {
    request =>
      Future {
        tryWithPickle {
          translateError(service.createFoodWithTempCode(read[NewMainFoodRecord](request.body)))
        }
      }
  }

  def updateMainFoodRecord(foodCode: String) = deadbolt.restrict(Roles.superuser)(parse.tolerantText) {
    request =>
      Future {
        tryWithPickle {
          translateError(service.updateMainFoodRecord(foodCode, read[MainFoodRecordUpdate](request.body)))
        }
      }
  }

  def updateLocalFoodRecord(foodCode: String, locale: String) = deadbolt.restrict(Roles.superuser)(parse.tolerantText) {
    request =>
      Future {
        tryWithPickle {
          translateError(service.updateLocalFoodRecord(foodCode, read[LocalFoodRecordUpdate](request.body), locale))
        }
      }
  }

  def deleteFood(code: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.deleteFoods(Seq(code)))
    }
  }
}
