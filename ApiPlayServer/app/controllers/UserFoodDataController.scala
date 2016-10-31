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

import scala.concurrent.Future

import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import security.DeadboltActionsAdapter
import security.Roles
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDatabaseService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.InheritableAttributeSource
import uk.ac.ncl.openlab.intake24.services.fooddb.user.SourceLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.user.SourceRecord
import upickle._
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageService

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
      case SourceRecord.NoRecord => Js.Obj(("source", Js.Str("none")))
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

case class UserAsServedImageWithUrls(mainImageUrl: String, thumbnailUrl: String, weight: Double)

case class UserAsServedSetWithUrls(selectionImageUrl: String, images: Seq[UserAsServedImageWithUrls])

class UserFoodDataController @Inject() (service: FoodDatabaseService, deadbolt: DeadboltActionsAdapter, imageStorageService: ImageStorageService) extends Controller with FoodDatabaseErrorHandler {

  def getCategoryContents(code: String, locale: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getCategoryContents(code, locale))
    }
  }

  def getFoodData(code: String, locale: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getFoodData(code, locale))
    }
  }

  def getFoodDataWithSources(code: String, locale: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getFoodData(code, locale))
    }
  }

  def getAssociatedFoodPrompts(code: String, locale: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getAssociatedFoods(code, locale))
    }
  }

  def getBrandNames(code: String, locale: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getBrandNames(code, locale))
    }
  }

  def getAsServedSet(id: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getAsServedSet(id).right.map {
        set =>
        val images = set.images.map {
          image => UserAsServedImageWithUrls(imageStorageService.getUrl(image.mainImagePath), imageStorageService.getUrl(image.thumbnailPath), image.weight)
        }
        
        UserAsServedSetWithUrls(imageStorageService.getUrl(set.selectionImagePath), images)
      })
    }
  }

  def getDrinkwareSet(id: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getDrinkwareSet(id))
    }
  }

  def getGuideImage(id: String) = deadbolt.restrict(Roles.superuser) {
    Future {
      translateResult(service.getGuideImage(id))
    }
  }
}
