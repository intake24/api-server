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

import io.circe.generic.auto._
import parsers.JsonUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.{LocalFoodRecordUpdate, MainFoodRecordUpdate, NewLocalMainFoodRecord, NewMainFoodRecord}

import scala.concurrent.Future

class FoodsAdminController @Inject()(service: FoodsAdminService,
                                     foodAuthChecks: FoodAuthChecks,
                                     rab: Intake24RestrictedActionBuilder) extends Controller
  with DatabaseErrorHandler with JsonUtils {

  def getFoodRecord(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canReadFoods(locale)) {
    Future {
      translateDatabaseResult(service.getFoodRecord(code, locale))
    }
  }

  def isFoodCodeAvailable(code: String) = rab.restrictAccess(foodAuthChecks.canCheckFoodCodes) {
    Future {
      translateDatabaseResult(service.isFoodCodeAvailable(code))
    }
  }

  def isFoodCode(code: String) = rab.restrictAccess(foodAuthChecks.canCheckFoodCodes) {
    Future {
      translateDatabaseResult(service.isFoodCode(code))
    }
  }

  def createFood() = rab.restrictAccess(foodAuthChecks.canCreateMainFoods)(jsonBodyParser[NewMainFoodRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createFood(request.body))
      }
  }

  def createLocalFood(localeId: String) = rab.restrictAccess(foodAuthChecks.canCreateLocalFoods(localeId))(jsonBodyParser[NewLocalMainFoodRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createFood(NewMainFoodRecord(request.body.code, request.body.englishDescription, request.body.groupCode,
          request.body.attributes, request.body.parentCategories, Seq(localeId))))
      }
  }

  def createFoodWithTempCode() = rab.restrictAccess(foodAuthChecks.canCreateMainFoods)(jsonBodyParser[NewMainFoodRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createFoodWithTempCode(request.body))
      }
  }

  def updateMainFoodRecord(foodCode: String) = rab.restrictAccessWithDatabaseCheck(foodAuthChecks.canUpdateMainFood(foodCode))(jsonBodyParser[MainFoodRecordUpdate]) {
    request =>
      Future {
        if (!foodAuthChecks.isFoodsAdmin(request.subject) && !request.body.localeRestrictions.forall(l => foodAuthChecks.isLocaleMaintainer(l, request.subject)))
          Forbidden
        else
          translateDatabaseResult(service.updateMainFoodRecord(foodCode, request.body))
      }
  }

  def updateLocalFoodRecord(foodCode: String, locale: String) = rab.restrictAccess(foodAuthChecks.canUpdateLocalFoods(locale))(jsonBodyParser[LocalFoodRecordUpdate]) {
    request =>
      Future {
        translateDatabaseResult(service.updateLocalFoodRecord(foodCode, request.body, locale))
      }
  }

  def deleteFood(code: String) = rab.restrictAccess(foodAuthChecks.canDeleteFoods) {
    Future {
      translateDatabaseResult(service.deleteFoods(Seq(code)))
    }
  }
}
