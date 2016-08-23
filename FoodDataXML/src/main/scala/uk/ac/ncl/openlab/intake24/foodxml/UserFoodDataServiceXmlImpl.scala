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

package uk.ac.ncl.openlab.intake24.foodxml

import org.slf4j.LoggerFactory
import com.google.inject.Inject
import com.google.inject.Singleton
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.InheritableAttributes

import uk.ac.ncl.openlab.intake24.UserCategoryContents
import uk.ac.ncl.openlab.intake24.UserCategoryHeader

import uk.ac.ncl.openlab.intake24.services.foodindex.Util.mkHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError

import uk.ac.ncl.openlab.intake24.UserFoodData

import uk.ac.ncl.openlab.intake24.UserFoodHeader
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDatabaseService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound

@Singleton
class UserFoodDataServiceXmlImpl @Inject() (data: XmlDataSource) extends FoodDatabaseService {

  import Util._

  val defaultLocale = "en_GB"

  val log = LoggerFactory.getLogger(classOf[UserFoodDataServiceXmlImpl])

  def checkLocale(locale: String) = if (locale != defaultLocale)
    log.warn("Locales other than en_GB are not supported by this implementation -- returning en_GB results for debug purposes");

  def rootCategories(locale: String): Either[DatabaseError, Seq[UserCategoryHeader]] = {
    checkLocale(locale)
    Right(data.categories.rootCategories.map(mkHeader))
  }

  def categoryContents(code: String, locale: String) = {
    checkLocale(locale)

    val cat = data.categories.find(code)

    val foodHeaders = cat.foods.map(fcode => mkHeader(data.foods.find(fcode)))
    val categoryHeaders = cat.subcategories.map(catcode => mkHeader(data.categories.find(catcode)))

    Right(UserCategoryContents(foodHeaders, categoryHeaders))
  }
  
  def foodAllCategories(code: String) = Right(data.categories.foodAllCategories(code).toSet)   
  
  def categoryAllCategories(code: String) = Right(data.categories.categoryAllCategories(code).toSet)
  
  def isCategoryCode(code: String) = data.categories.categoryMap.contains(code)
  
  def foodData(code: String, locale: String) = {
    checkLocale(locale)

    val f = data.foods.find(code)

    val portionSizeMethods = {
      val ps = f.local.portionSize
      if (ps.isEmpty) {
        data.inheritance.foodInheritedPortionSize(code)
      } else
        ps
    }

    val readyMealOption = f.main.attributes.readyMealOption match {
      case Some(value) => value
      case None => data.inheritance.foodInheritedAttribute(code, _.readyMealOption) match {
        case Some(value) => value
        case None => InheritableAttributes.readyMealDefault
      }
    }

    val sameAsBeforeOption = f.main.attributes.sameAsBeforeOption match {
      case Some(value) => value
      case None => data.inheritance.foodInheritedAttribute(code, _.sameAsBeforeOption) match {
        case Some(value) => value
        case None => InheritableAttributes.sameAsBeforeDefault
      }
    }

    val reasonableAmount = f.main.attributes.reasonableAmount match {
      case Some(value) => value
      case None => data.inheritance.foodInheritedAttribute(code, _.reasonableAmount) match {
        case Some(value) => value
        case None => InheritableAttributes.reasonableAmountDefault
      }
    }

    Right((UserFoodData(f.main.code, f.main.englishDescription, f.local.nutrientTableCodes, f.main.groupCode, portionSizeMethods, readyMealOption, sameAsBeforeOption, reasonableAmount), null))
  }

  def getAsServedSet(id: String) = data.asServedSets.get(id) match {
    case Some(set) => Right(set)
    case None => Left(RecordNotFound)
  }

  def getGuideImage(id: String) = data.guideImages.get(id) match {
    case Some(image) => Right(image)
    case None => Left(RecordNotFound)
  }

  def getDrinkwareSet(id: String) = data.drinkwareSets.get(id) match {
    case Some(set) => Right(set)
    case None => Left(RecordNotFound)
  }

  def associatedFoods(foodCode: String, locale: String) = {
    checkLocale(locale)
    data.prompts.get(foodCode) match {
      case Some(seq) => {
        Right(seq.map {
          v1 =>
            data.categories.categoryMap.get(v1.category) match {
              case Some(category) => AssociatedFood(Right(category.code), v1.promptText, v1.linkAsMain, v1.genericName)
              case None => {
                val food = data.foods.find(v1.category)
                AssociatedFood(Left(food.main.code), v1.promptText, v1.linkAsMain, v1.genericName)
              }
            }
        })
      }
      case None => Left(RecordNotFound)
    }
  }

  def getBrandNames(foodCode: String, locale: String) = {
    checkLocale(locale)
    data.brandNamesMap.get(foodCode) match {
      case Some(map) => Right(map)
      case None => Left(RecordNotFound)
    }
  }

}