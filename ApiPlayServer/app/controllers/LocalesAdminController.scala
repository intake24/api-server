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
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.LocalesAdminService
import upickle.default._
import security.Roles
import scala.concurrent.Future

class LocalesAdminController @Inject() (service: LocalesAdminService, deadbolt: DeadboltActionsAdapter) extends Controller with ApiErrorHandler {
  
  def listLocales() = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.listLocales())
    }
  }
  
  def getLocale(id: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateError(service.getLocale(id))
    }
  }
}
