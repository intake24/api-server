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
import parsers.{JsonBodyParser, JsonUtils}
import play.api.mvc.{BaseController, ControllerComponents, PlayBodyParsers}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.api.data.admin._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{AssociatedFoodsService, BrandNamesService, FoodDataService}

import scala.concurrent.{ExecutionContext, Future}

case class CloneFoodResult(clonedFoodCode: String)

class FoodsAdminController @Inject()(service: FoodsAdminService,
                                     userService: FoodDataService,
                                     associatedFoodsService: AssociatedFoodsService,
                                     brandNamesService: BrandNamesService,
                                     foodAuthChecks: FoodAuthChecks,
                                     rab: Intake24RestrictedActionBuilder,
                                     bodyParsers: PlayBodyParsers,
                                     jsonBodyParser: JsonBodyParser,
                                     val controllerComponents: ControllerComponents,
                                     implicit val executionContext: ExecutionContext) extends BaseController
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

  def createFood() = rab.restrictAccess(foodAuthChecks.canCreateMainFoods)(jsonBodyParser.parse[NewMainFoodRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createFood(request.body))
      }
  }

  def addFoodToLocalList(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canCreateLocalFoods(locale))(bodyParsers.empty) {
    request =>
      Future {
        translateDatabaseResult(service.addFoodToLocalList(code, locale))
      }
  }

  def cloneFood(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canCreateMainFoods)(bodyParsers.empty) {
    _ =>
      Future {
        val result =
          for (
            sourceFoodRecord <- service.getFoodRecord(code, locale).right;
            code <- service.createFoodWithTempCode(NewMainFoodRecord("TEMP", "Copy of " + sourceFoodRecord.main.englishDescription,
              sourceFoodRecord.main.groupCode, sourceFoodRecord.main.attributes, sourceFoodRecord.main.parentCategories.map(_.code),
              sourceFoodRecord.main.localeRestrictions)).right;
            _ <- service.updateLocalFoodRecord(code, LocalFoodRecordUpdate(None, sourceFoodRecord.local.localDescription.map("Copy of " + _),
              sourceFoodRecord.local.nutrientTableCodes, sourceFoodRecord.local.portionSize,
              sourceFoodRecord.local.associatedFoods.map(_.toAssociatedFood), sourceFoodRecord.local.brandNames), locale).right;
            _ <- service.addFoodToLocalList(code, locale)
          )
            yield CloneFoodResult(code)

        translateDatabaseResult(result)
      }
  }

  def cloneFoodAsLocal(code: String, locale: String) = rab.restrictAccess(foodAuthChecks.canCreateLocalFoods(locale))(bodyParsers.empty) {
    _ =>
      Future {
        val result =
          for (
            sourceFoodRecord <- service.getFoodRecord(code, locale).right;
            sourceUserRecord <- userService.getFoodData(code, locale).right.map(_._1).right;
            assocFoods <- associatedFoodsService.getAssociatedFoods(code, locale).right;
            brandNames <- brandNamesService.getBrandNames(code, locale).right;
            code <- service.createFoodWithTempCode(NewMainFoodRecord("TEMP", "Copy of " + sourceFoodRecord.main.englishDescription,
              sourceFoodRecord.main.groupCode, sourceFoodRecord.main.attributes, sourceFoodRecord.main.parentCategories.map(_.code),
              Seq(locale))).right;

            _ <- service.updateLocalFoodRecord(code, LocalFoodRecordUpdate(None, Some("Copy of " + sourceUserRecord.localDescription),
              sourceUserRecord.nutrientTableCodes, sourceUserRecord.portionSizeMethods,
              assocFoods, brandNames), locale).right;
            _ <- service.addFoodToLocalList(code, locale)
          )

            yield CloneFoodResult(code)

        translateDatabaseResult(result)
      }
  }

  def createLocalFood(localeId: String) = rab.restrictAccess(foodAuthChecks.canCreateLocalFoods(localeId))(jsonBodyParser.parse[NewLocalMainFoodRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createFood(NewMainFoodRecord(request.body.code, request.body.englishDescription, request.body.groupCode,
          request.body.attributes, request.body.parentCategories, Seq(localeId))))
      }
  }

  def createFoodWithTempCode() = rab.restrictAccess(foodAuthChecks.canCreateMainFoods)(jsonBodyParser.parse[NewMainFoodRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createFoodWithTempCode(request.body))
      }
  }

  def updateMainFoodRecord(foodCode: String) = rab.restrictAccessWithDatabaseCheck(foodAuthChecks.canUpdateMainFood(foodCode))(jsonBodyParser.parse[MainFoodRecordUpdate]) {
    request =>
      Future {
        if (!foodAuthChecks.isFoodsAdmin(request.subject) && !request.body.localeRestrictions.forall(l => foodAuthChecks.isLocaleMaintainer(l, request.subject)))
          Forbidden
        else
          translateDatabaseResult(service.updateMainFoodRecord(foodCode, request.body))
      }
  }

  def updateLocalFoodRecord(foodCode: String, locale: String) = rab.restrictAccess(foodAuthChecks.canUpdateLocalFoods(locale))(jsonBodyParser.parse[LocalFoodRecordUpdate]) {
    request =>
      Future {
        translateDatabaseResult(service.updateLocalFoodRecord(foodCode, request.body, locale))
      }
  }

  def deleteFood(code: String) = rab.restrictAccessWithDatabaseCheck(foodAuthChecks.canDeleteFood(code)) {
    Future {
      translateDatabaseResult(service.deleteFoods(Seq(code)))
    }
  }
}
