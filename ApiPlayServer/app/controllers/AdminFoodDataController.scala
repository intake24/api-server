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
import play.api.http.ContentTypes
import play.api.libs.json.JsBoolean
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result
import security.Roles
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.services.AdminFoodDataService
import uk.ac.ncl.openlab.intake24.services.CodeError
import uk.ac.ncl.openlab.intake24.services.InvalidRequest
import uk.ac.ncl.openlab.intake24.services.NewCategory
import uk.ac.ncl.openlab.intake24.services.NewFood
import uk.ac.ncl.openlab.intake24.services.SqlException
import uk.ac.ncl.openlab.intake24.services.Success
import uk.ac.ncl.openlab.intake24.services.UpdateResult
import uk.ac.ncl.openlab.intake24.services.VersionConflict
import upickle.default.SeqishR
import upickle.default.SeqishW
import upickle.default.macroR
import upickle.default.macroW
import upickle.default.read
import upickle.default.write
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService
import models.AdminFoodRecord
import models.AdminCategoryRecord
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.services.LocaleManagementService

class AdminFoodDataController @Inject() (service: AdminFoodDataService, localeService: LocaleManagementService, userService: UserFoodDataService, deadbolt: DeadboltActions, cache: CacheApi) extends Controller with PickleErrorHandler {

  // Read 
  def rootCategories(locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.rootCategories(locale))).as(ContentTypes.JSON)
    }
  }

  def uncategorisedFoods(locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.uncategorisedFoods(locale))).as(ContentTypes.JSON)
    }
  }

  def categoryContents(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.categoryContents(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodParentCategories(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.foodParentCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodAllCategories(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.foodAllCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodRecord(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      val result = for {
        record <- service.foodRecord(code, locale).right
        brandNames <- userService.brandNames(code, locale).right
        associatedFoods <- userService.associatedFoods(code, locale).right
      } yield AdminFoodRecord(record.main, record.local, brandNames, associatedFoods)

      result match {
        case Left(CodeError.UndefinedCode) => NotFound
        case Right(record) => Ok(write(record)).as(ContentTypes.JSON)
      }
    }
  }

  def categoryRecord(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      service.categoryRecord(code, locale) match {
        case Left(CodeError.UndefinedCode) => NotFound
        case Right(record) => Ok(write(AdminCategoryRecord(record.main, record.local))).as(ContentTypes.JSON)
      }
    }
  }

  def categoryParentCategories(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.categoryParentCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def categoryAllCategories(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.categoryAllCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def allAsServedSets() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.allAsServedSets())).as(ContentTypes.JSON)
    }
  }

  def allDrinkware = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.allDrinkware())).as(ContentTypes.JSON)
    }
  }

  def allGuideImages() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.allGuideImages())).as(ContentTypes.JSON)
    }
  }

  def allFoodGroups(locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.allFoodGroups(locale))).as(ContentTypes.JSON)
    }
  }

  def foodGroup(id: Int, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      service.foodGroup(id, locale) match {
        case Some(group) => Ok(write(group)).as(ContentTypes.JSON)
        case None => BadRequest
      }
    }
  }

  def searchFoods(searchTerm: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.searchFoods(searchTerm, locale))).as(ContentTypes.JSON)
    }
  }

  def searchCategories(searchTerm: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.searchCategories(searchTerm, locale))).as(ContentTypes.JSON)
    }
  }

  def nutrientTables() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.nutrientTables())).as(ContentTypes.JSON)
    }
  }

  // Write

  def localFoodRecordCacheKey(code: String, locale: String) = s"food.local.$locale.$code"

  def localCategoryRecordCacheKey(code: String, locale: String) = s"category.local.$locale.$code"

  def mainFoodRecordCacheKey(code: String) = s"food.main.$code"

  def mainCategoryRecordCacheKey(code: String) = s"category.main.$code"

  def invalidateLocalFoodRecord(code: String, locale: String) = cache.remove(localFoodRecordCacheKey(code, locale))

  def invalidateMainFoodRecord(code: String) = cache.remove(mainFoodRecordCacheKey(code))

  def invalidateProblemsForFood(code: String, locale: String) = {
    cache.remove(ProblemChecker.foodProblemsCacheKey(code, locale))
    service.foodAllCategories(code, locale).foreach {
      header =>
        cache.remove(ProblemChecker.recursiveCategoryProblemsCacheKey(header.code, locale))
        cache.remove(ProblemChecker.categoryProblemsCacheKey(header.code, locale))
    }
  }

  def invalidateProblemsForCategory(code: String, locale: String) = {
    cache.remove(ProblemChecker.categoryProblemsCacheKey(code, locale))
    cache.remove(ProblemChecker.recursiveCategoryProblemsCacheKey(code, locale))
    service.categoryAllCategories(code, locale).foreach {
      header =>
        cache.remove(ProblemChecker.recursiveCategoryProblemsCacheKey(header.code, locale))
        cache.remove(ProblemChecker.categoryProblemsCacheKey(header.code, locale))
    }
  }

  def translateUpdateResult(result: UpdateResult): Result = result match {
    case Success => Ok
    case VersionConflict => Conflict
    case InvalidRequest(errorCode, message) => BadRequest(Json.obj("error" -> "invalid_request", "code" -> errorCode, "message" -> message))
    case SqlException(message) => InternalServerError(Json.obj("error" -> "sql_exception", "message" -> message))
  }

  def updateFoodBase(foodCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        val result = service.updateFoodBase(foodCode, read[MainFoodRecord](request.body))

        if (result == Success) {
          
          invalidateMainFoodRecord(foodCode)
          
          val categories = service.foodAllCategories(foodCode)

          localeService.list.foreach {
            locale =>
              invalidateProblemsForFood(foodCode, locale.id)
          }
        }

        translateUpdateResult(result)
      }
    }
  }

  def updateFoodLocal(foodCode: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        val result = service.updateFoodLocal(foodCode, locale, read[LocalFoodRecord](request.body))

        if (result == Success) {
          invalidateLocalFoodRecord(foodCode, locale)
          invalidateProblemsForFood(foodCode, locale)
        }

        translateUpdateResult(result)
      }
    }
  }

  def updateAssociatedFoodPrompts(foodCode: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.updateAssociatedFoods(foodCode, locale, read[Seq[AssociatedFood]](request.body)))
      }
    }
  }

  def isFoodCodeAvailable(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(JsBoolean(service.isFoodCodeAvailable(code)))
    }
  }

  def createFood() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.createFood(read[NewFood](request.body)))
      }
    }
  }

  def createFoodWithTempCode() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        service.createFoodWithTempCode(read[NewFood](request.body)) match {
          case Left(error) => translateUpdateResult(error)
          case Right(tempCode) => Ok(tempCode)
        }
      }
    }
  }

  def deleteFood(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {

      // Race condition :(

      val categories = service.foodAllCategories(code)

      val result = service.deleteFood(code)

      if (result == Success) {
        invalidateMainFoodRecord(code)

        localeService.list.foreach {
          locale =>
            invalidateLocalFoodRecord(code, locale.id)
            invalidateProblemsForFood(code, locale.id)

            categories.foreach {
              categoryCode =>
                invalidateProblemsForCategory(categoryCode, locale.id)
            }
        }
      }

      translateUpdateResult(result)
    }
  }

  def addFoodToCategory(categoryCode: String, foodCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      val result = service.addFoodToCategory(categoryCode, foodCode)

      if (result == Success) {
        localeService.list.foreach {
          locale =>
            invalidateProblemsForFood(foodCode, locale.id)
            invalidateProblemsForCategory(categoryCode, locale.id)
        }
      }

      translateUpdateResult(result)
    }
  }

  def removeFoodFromCategory(categoryCode: String, foodCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      val result = service.removeFoodFromCategory(categoryCode, foodCode)

      if (result == Success) {
        localeService.list.foreach {
          locale =>
            invalidateProblemsForFood(foodCode, locale.id)
            invalidateProblemsForCategory(categoryCode, locale.id)
        }
      }

      translateUpdateResult(result)
    }
  }

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      val result = service.addSubcategoryToCategory(categoryCode, subcategoryCode)

      if (result == Success) {
        localeService.list.foreach {
          locale =>
            invalidateProblemsForCategory(categoryCode, locale.id)
            invalidateProblemsForCategory(subcategoryCode, locale.id)
        }
      }

      translateUpdateResult(result)
    }
  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      val result = service.removeSubcategoryFromCategory(categoryCode, subcategoryCode)
      
      if (result == Success) {
        localeService.list.foreach {
          locale =>
            invalidateProblemsForCategory(categoryCode, locale.id)
            invalidateProblemsForCategory(subcategoryCode, locale.id)
        }        
      }
      
      translateUpdateResult(result)
    }
  }

  def isCategoryCodeAvailable(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(JsBoolean(service.isCategoryCodeAvailable(code)))
    }
  }

  def createCategory() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.createCategory(read[NewCategory](request.body)))
      }
    }
  }

  def updateCategoryBase(categoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        
        val result = service.updateCategoryBase(categoryCode, read[MainCategoryRecord](request.body))
        
        if (result == Success) {
          
        }
        
        translateUpdateResult(result)
      }
    }
  }

  def updateCategoryLocal(categoryCode: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.updateCategoryLocal(categoryCode, locale, read[LocalCategoryRecord](request.body)))
      }
    }
  }

  def deleteCategory(categoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateResult(service.deleteCategory(categoryCode))
    }
  }
}
