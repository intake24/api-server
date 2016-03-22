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

import be.objectify.deadbolt.core.PatternType
import be.objectify.deadbolt.scala.DeadboltActions
import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.mvc.Action
import play.api.mvc.Controller
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService
import upickle.default._
import uk.ac.ncl.openlab.intake24.services.FoodDataError
import upickle.Js
import uk.ac.ncl.openlab.intake24.services.FoodDataSources
import uk.ac.ncl.openlab.intake24.services.SourceLocale
import uk.ac.ncl.openlab.intake24.services.SourceRecord
import uk.ac.ncl.openlab.intake24.services.InheritableAttributeSource

object FoodSourceWriters {
  implicit val sourceLocaleWriter = upickle.default.Writer[SourceLocale] {
    case t => t match {
      case SourceLocale.Current(locale) => Js.Obj(("source", Js.Str("current")), ("id", Js.Str(locale)))
      case SourceLocale.Prototype(locale) => Js.Obj(("source", Js.Str("prototype")), ("id", Js.Str(locale)))
      case SourceLocale.Fallback(locale) => Js.Obj(("source", Js.Str("fallback")), ("id", Js.Str(locale)))
    }
  }

  implicit val sourceRecordWriter = upickle.default.Writer[SourceRecord] {
    case t => t match {
      case SourceRecord.CategoryRecord(code) => Js.Obj(("source", Js.Str("category")), ("code", Js.Str(code)))
      case SourceRecord.FoodRecord(code) => Js.Obj(("source", Js.Str("food")), ("code", Js.Str(code)))
    }
  }

  implicit val inheritableAttributeSourceWriter = upickle.default.Writer[InheritableAttributeSource] {
    case t => t match {
      case InheritableAttributeSource.FoodRecord(code: String) => Js.Obj(("source", Js.Str("food")), ("code", Js.Str(code)))
      case InheritableAttributeSource.CategoryRecord(code: String) => Js.Obj(("source", Js.Str("category")), ("code", Js.Str(code)))
      case InheritableAttributeSource.Default => Js.Obj(("source", Js.Str("default")))
    }
  }
}

class UserFoodDataController @Inject() (service: UserFoodDataService, deadbolt: DeadboltActions) extends Controller {

  import FoodSourceWriters._
  
  def categoryContents(code: String, locale: String) = deadbolt.Pattern("api.fooddata.user", PatternType.EQUALITY) {
    Action {
      Ok(write(service.categoryContents(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodData(code: String, locale: String) = deadbolt.Pattern("api.fooddata.user", PatternType.EQUALITY) {
    Action {
      service.foodData(code, locale) match {
        case Left(_) => NotFound
        case Right((data, _)) => Ok(write(data)).as(ContentTypes.JSON)
      }
    }
  }

  def foodDataWithSources(code: String, locale: String) = deadbolt.Pattern("api.fooddata.user", PatternType.EQUALITY) {
    Action {
      service.foodData(code, locale) match {
        case Left(_) => NotFound
        case Right(data) => Ok(write(data)).as(ContentTypes.JSON)
      }
    }
  }

  def brandNames(code: String, locale: String) = deadbolt.Pattern("api.fooddata.user", PatternType.EQUALITY) {
    Action {
      Ok(write(service.brandNames(code, locale))).as(ContentTypes.JSON)
    }
  }

  def asServedDef(id: String) = deadbolt.Pattern("api.fooddata.user", PatternType.EQUALITY) {
    Action {
      Ok(write(service.asServedDef(id))).as(ContentTypes.JSON)
    }
  }

  def drinkwareDef(id: String) = deadbolt.Pattern("api.fooddata.user", PatternType.EQUALITY) {
    Action {
      Ok(write(service.drinkwareDef(id))).as(ContentTypes.JSON)
    }
  }

  def guideDef(id: String) = deadbolt.Pattern("api.fooddata.user", PatternType.EQUALITY) {
    Action {
      Ok(write(service.guideDef(id))).as(ContentTypes.JSON)
    }
  }

  def associatedFoodPrompts(code: String, locale: String) = deadbolt.Pattern("api.fooddata.user", PatternType.EQUALITY) {
    Action {
      Ok(write(service.associatedFoodPrompts(code, locale))).as(ContentTypes.JSON)
    }
  }
}
