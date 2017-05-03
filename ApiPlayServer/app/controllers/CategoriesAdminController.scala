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
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.{LocalCategoryRecordUpdate, MainCategoryRecordUpdate, NewLocalCategoryRecord, NewMainCategoryRecord}

import scala.concurrent.Future

class CategoriesAdminController @Inject()(service: CategoriesAdminService, rab: Intake24RestrictedActionBuilder) extends Controller
  with DatabaseErrorHandler with JsonUtils {

  def getCategoryRecord(code: String, locale: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.getCategoryRecord(code, locale))
    }
  }

  def isCategoryCodeAvailable(code: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.isCategoryCodeAvailable(code))
    }
  }

  def isCategoryCode(code: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.isCategoryCodeAvailable(code))
    }
  }

  def createMainCategoryRecord() = rab.restrictToRoles(Roles.superuser)(jsonBodyParser[NewMainCategoryRecord]) {
    request =>
      Future {
        translateDatabaseResult(service.createMainCategoryRecords(Seq(request.body)))
      }
  }

  def deleteCategory(categoryCode: String) = rab.restrictToRoles(Roles.superuser) {
    Future {
      translateDatabaseResult(service.deleteCategory(categoryCode))
    }
  }

  def updateMainCategoryRecord(categoryCode: String) = rab.restrictToRoles(Roles.superuser)(jsonBodyParser[MainCategoryRecordUpdate]) {
    request =>
      Future {
        translateDatabaseResult(service.updateMainCategoryRecord(categoryCode, request.body))
      }
  }

  def updateLocalCategoryRecord(categoryCode: String, locale: String) = rab.restrictToRoles(Roles.superuser)(jsonBodyParser[LocalCategoryRecordUpdate]) {
    request =>
      Future {
        val req = request.body

        // FIXME: Needs a better protocol
        req.baseVersion match {
          case Some(version) => translateDatabaseResult(service.updateLocalCategoryRecord(categoryCode, req, locale))
          case None => translateDatabaseResult(service.createLocalCategoryRecords(Map(categoryCode -> NewLocalCategoryRecord(req.localDescription, req.portionSize)), locale))
        }
      }
  }
}
