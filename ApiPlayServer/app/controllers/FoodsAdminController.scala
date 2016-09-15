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

import be.objectify.deadbolt.scala.DeadboltActions
import javax.inject.Inject
import models.AdminFoodRecord
import play.api.http.ContentTypes
import play.api.mvc.Action
import play.api.mvc.Controller
import security.Roles
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.NewMainFoodRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import upickle.default.Writer
import upickle.default.read
import upickle.default.write
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.MainFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.LocalFoodRecordUpdate

class FoodsAdminController @Inject() (service: FoodsAdminService, deadbolt: DeadboltActions) extends Controller
    with PickleErrorHandler
    with ApiErrorHandler {

  /*
   * 
   *   def getFoodRecord(code: String, locale: String): Either[LocalLookupError, FoodRecord]
  
  def isFoodCodeAvailable(code: String): Either[DatabaseError, Boolean]
  def isFoodCode(code: String): Either[DatabaseError, Boolean]

  def createFood(newFood: NewMainFoodRecord): Either[DependentCreateError, Unit]
  def createFoodWithTempCode(newFood: NewMainFoodRecord): Either[DependentCreateError, String]
  def createFoods(newFoods: Seq[NewMainFoodRecord]): Either[DependentCreateError, Unit]
  def createLocalFoods(localFoodRecords: Map[String, LocalFoodRecord], locale: String): Either[LocalDependentCreateError, Unit]

  def updateMainFoodRecord(foodCode: String, foodBase: MainFoodRecord): Either[UpdateError, Unit]
  def updateLocalFoodRecord(foodCode: String, locale: String, foodLocal: LocalFoodRecord): Either[LocalUpdateError, Unit]

  def deleteAllFoods(): Either[DatabaseError, Unit]
  def deleteFood(foodCode: String): Either[DeleteError, Unit]  
   * 
   */

  def getFoodRecord(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateLocalLookupError(service.getFoodRecord(code, locale))
    }
  }

  def isFoodCodeAvailable(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateDatabaseError(service.isFoodCodeAvailable(code))
    }
  }

  def isFoodCode(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateDatabaseError(service.isFoodCode(code))
    }
  }

  def createFood() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateDependentCreateError(service.createFood(read[NewMainFoodRecord](request.body)))
      }
    }
  }

  def createFoodWithTempCode() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateDependentCreateError(service.createFoodWithTempCode(read[NewMainFoodRecord](request.body)))
      }
    }
  }

  def updateMainFoodRecord(foodCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateLocalDependentUpdateError(service.updateMainFoodRecord(foodCode, read[MainFoodRecordUpdate](request.body)))
      }
    }
  }

  def updateLocalFoodRecord(foodCode: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateLocalDependentUpdateError(service.updateLocalFoodRecord(foodCode, read[LocalFoodRecordUpdate](request.body), locale))
      }
    }
  }

  def deleteFood(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateDeleteError(service.deleteFoods(Seq(code)))
    }
  }

}
