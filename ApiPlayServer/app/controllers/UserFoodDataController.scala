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

import upickle.default._

import upickle.Js
import security.Roles
import uk.ac.ncl.openlab.intake24.services.fooddb.user.SourceLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.user.SourceRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.user.SourceRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.user.InheritableAttributeSource
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDatabaseService

object FoodSourceWriters {
  implicit val sourceLocaleWriter = upickle.default.Writer[SourceLocale] {
    case t => t match {
      case SourceLocale.Current(locale) => Js.Obj(("source", Js.Str("current")), ("id", Js.Str(locale)))
      case SourceLocale.Prototype(locale) => Js.Obj(("source", Js.Str("prototype")), ("id", Js.Str(locale)))
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

class UserFoodDataController @Inject() (service: FoodDatabaseService, deadbolt: DeadboltActions) extends Controller with ApiErrorHandler {

  import FoodSourceWriters._

  def categoryContents(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateResult(service.categoryContents(code, locale))
    }
  }

  def foodData(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateResult(service.foodData(code, locale))
    }
  }

  def foodDataWithSources(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateResult(service.foodData(code, locale))
    }
  }

  def associatedFoodPrompts(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateResult(service.getAssociatedFoods(code, locale))
    }
  }

  def brandNames(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateResult(service.getBrandNames(code, locale))
    }
  }

  def asServedDef(id: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateResult(service.getAsServedSet(id))
    }
  }

  def drinkwareDef(id: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateResult(service.getDrinkwareSet(id))
    }
  }

  def guideDef(id: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateResult(service.getGuideImage(id))
    }
  }
}
