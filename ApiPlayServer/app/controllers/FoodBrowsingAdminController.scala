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
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.Intake24RestrictedActionBuilder
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodBrowsingAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles

import scala.concurrent.Future

class FoodBrowsingAdminController @Inject()(service: FoodBrowsingAdminService, rab: Intake24RestrictedActionBuilder) extends Controller
  with DatabaseErrorHandler {

  def getUncategorisedFoods(locale: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getUncategorisedFoods(locale))
    }
  }

  def getRootCategories(locale: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getRootCategories(locale))
    }
  }

  def getCategoryContents(code: String, locale: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getCategoryContents(code, locale))
    }
  }

  def getFoodParentCategories(code: String, locale: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getFoodParentCategories(code, locale))
    }
  }

  def getFoodAllCategories(code: String, locale: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getFoodAllCategoriesHeaders(code, locale))
    }
  }

  def getCategoryParentCategories(code: String, locale: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getCategoryParentCategories(code, locale))
    }
  }

  def getCategoryAllCategories(code: String, locale: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getCategoryAllCategoriesHeaders(code, locale))
    }
  }

}
