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

import javax.inject.Inject

import parsers.JsonUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.{LocalFoodRecordUpdate, MainFoodRecordUpdate, NewMainFoodRecord}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import io.circe.generic.auto._

import scala.concurrent.Future

class FoodsAdminController @Inject() (service: FoodsAdminService, deadbolt: DeadboltActionsAdapter) extends Controller
    with DatabaseErrorHandler with JsonUtils {

  def getFoodRecord(code: String, locale: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getFoodRecord(code, locale))
    }
  }

  def isFoodCodeAvailable(code: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.isFoodCodeAvailable(code))
    }
  }

  def isFoodCode(code: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.isFoodCode(code))
    }
  }

  def createFood() = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[NewMainFoodRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createFood(request.body))
      }
  }

  def createFoodWithTempCode() = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[NewMainFoodRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createFoodWithTempCode(request.body))
      }
  }

  def updateMainFoodRecord(foodCode: String) = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[MainFoodRecordUpdate]) {
    request =>
      Future {
        translateDatabaseResult(service.updateMainFoodRecord(foodCode, request.body))
      }
  }

  def updateLocalFoodRecord(foodCode: String, locale: String) = deadbolt.restrictToRoles(Roles.superuser)(jsonBodyParser[LocalFoodRecordUpdate]) {
    request =>
      Future {
        translateDatabaseResult(service.updateLocalFoodRecord(foodCode, request.body, locale))
      }
  }

  def deleteFood(code: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.deleteFoods(Seq(code)))
    }
  }
}
