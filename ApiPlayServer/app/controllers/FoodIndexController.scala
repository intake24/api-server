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
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.{DeadboltActionsAdapter, Roles}
import uk.ac.ncl.openlab.intake24.services.foodindex.FoodIndex

import scala.concurrent.Future

class FoodIndexController @Inject()(foodIndexes: Map[String, FoodIndex], deadbolt: DeadboltActionsAdapter) extends Controller with JsonUtils {
  def lookup(locale: String, term: String) = deadbolt.restrictToRoles(Roles.superuser) {
    Future {
      foodIndexes.get(locale) match {
        case Some(index) => Ok(toJsonString(index.lookup(term, 50))).as(ContentTypes.JSON)
        case None => BadRequest(s"Food index not configured for locale $locale")
      }
    }
  }
}
