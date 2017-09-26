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
import play.api.mvc.{BaseController, ControllerComponents}
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService

import scala.concurrent.{ExecutionContext, Future}

class FoodGroupsAdminController @Inject()(service: FoodGroupsAdminService,
                                          foodAuthChecks: FoodAuthChecks,
                                          rab: Intake24RestrictedActionBuilder,
                                          val controllerComponents: ControllerComponents,
                                          implicit val executionContext: ExecutionContext) extends BaseController
  with DatabaseErrorHandler {

  def listFoodGroups(locale: String) = rab.restrictAccess(foodAuthChecks.canReadFoodGroups) {
    Future {
      // Map keys to Strings to force upickle to use js object serialisation instead of array of arrays
      translateDatabaseResult(service.listFoodGroups(locale).right.map(_.map { case (k, v) => (k.toString, v) }))
    }
  }

  def getFoodGroup(id: Int, locale: String) = rab.restrictAccess(foodAuthChecks.canReadFoodGroups) {
    Future {
      translateDatabaseResult(service.getFoodGroup(id, locale))
    }
  }
}
