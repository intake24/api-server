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
import uk.ac.ncl.openlab.intake24.NewFood
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

class CategoriesAdminController @Inject() (service: CategoriesAdminService, deadbolt: DeadboltActions) extends Controller
    with PickleErrorHandler
    with ApiErrorHandler {

  def getCategoryRecord(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateLocalLookupError(service.getCategoryRecord(code, locale))
    }
  }
  def isCategoryCodeAvailable(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateDatabaseError(service.isCategoryCodeAvailable(code))
    }
  }

  def isCategoryCode(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateDatabaseError(service.isCategoryCodeAvailable(code))
    }
  }

  def createCategory() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateCreateError(service.createCategory(read[NewCategory](request.body)))
      }
    }
  }

  def createCategories() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateCreateError(service.createCategories(read[Seq[NewCategory]](request.body)))
      }
    }
  }

  def deleteCategory(categoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateDeleteError(service.deleteCategory(categoryCode))
    }
  }

  def updateMainCategoryRecord(categoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateError(service.updateMainCategoryRecord(categoryCode, read[MainCategoryRecord](request.body)))
      }
    }
  }

  def updateLocalCategoryRecord(categoryCode: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateLocalUpdateError(service.updateLocalCategoryRecord(categoryCode, locale, read[LocalCategoryRecord](request.body)))
      }
    }
  }

  def addFoodToCategory(categoryCode: String, foodCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateError(service.addFoodToCategory(categoryCode, foodCode))

    }
  }
  def removeFoodFromCategory(categoryCode: String, foodCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateError(service.removeFoodFromCategory(categoryCode, foodCode))
    }
  }

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateError(service.addSubcategoryToCategory(categoryCode, subcategoryCode))
    }
  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateError(service.removeSubcategoryFromCategory(categoryCode, subcategoryCode))
    }
  }
}
