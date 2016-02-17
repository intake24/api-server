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
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.FoodData
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.InheritableAttributes
import net.scran24.fooddef.Prompt
import net.scran24.fooddef.UserCategoryContents
import net.scran24.fooddef.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService
import uk.ac.ncl.openlab.intake24.services.foodindex.Util.mkHeader
import uk.ac.ncl.openlab.intake24.services.ResourceNotFound
import uk.ac.ncl.openlab.intake24.services.UndefinedCode

@Singleton
class UserFoodDataServiceXmlImpl @Inject() (data: XmlDataSource) extends UserFoodDataService {

  import Util._

  val defaultLocale = "en_GB"
   
  val log = LoggerFactory.getLogger(classOf[UserFoodDataServiceXmlImpl])
  
  def checkLocale(locale: String) = if (locale != defaultLocale)
    log.warn("Locales other than en_GB are not supported by this implementation -- returning en_GB results for debug purposes");

  def rootCategories(locale: String): Seq[UserCategoryHeader] = {
    checkLocale(locale)
    data.categories.rootCategories.map(mkHeader)
  }

  def categoryContents(code: String, locale: String) = {
    checkLocale(locale)

    val cat = data.categories.find(code)

    val foodHeaders = cat.foods.map(fcode => mkHeader(data.foods.find(fcode)))
    val categoryHeaders = cat.subcategories.map(catcode => mkHeader(data.categories.find(catcode)))

    Right(UserCategoryContents(foodHeaders, categoryHeaders))
  }

  def foodData(code: String, locale: String) = {
    checkLocale(locale)

    val f = data.foods.find(code)

    val portionSizeMethods = {
      val ps = f.localData.portionSize
      if (ps.isEmpty) {
        data.inheritance.foodInheritedPortionSize(code)
      } else
        ps
    }

    val readyMealOption = f.attributes.readyMealOption match {
      case Some(value) => value
      case None => data.inheritance.foodInheritedAttribute(code, _.readyMealOption) match {
        case Some(value) => value
        case None => InheritableAttributes.readyMealDefault
      }
    }

    val sameAsBeforeOption = f.attributes.sameAsBeforeOption match {
      case Some(value) => value
      case None => data.inheritance.foodInheritedAttribute(code, _.sameAsBeforeOption) match {
        case Some(value) => value
        case None => InheritableAttributes.sameAsBeforeDefault
      }
    }

    val reasonableAmount = f.attributes.reasonableAmount match {
      case Some(value) => value
      case None => data.inheritance.foodInheritedAttribute(code, _.reasonableAmount) match {
        case Some(value) => value
        case None => InheritableAttributes.reasonableAmountDefault
      }
    }

    Right(FoodData(f.code, f.englishDescription, f.localData.nutrientTableCodes, f.groupCode, portionSizeMethods, readyMealOption, sameAsBeforeOption, reasonableAmount))
  }

  def asServedDef(id: String) = data.asServedSets.get(id) match {
    case Some(set) => Right(set)
    case None => Left(ResourceNotFound)
  }

  def guideDef(id: String) = data.guideImages.get(id) match {
    case Some(image) => Right(image)
    case None => Left(ResourceNotFound)
  }

  def drinkwareDef(id: String) = data.drinkwareSets.get(id) match {
    case Some(set) => Right(set)
    case None => Left(ResourceNotFound)
  }

  def associatedFoodPrompts(foodCode: String, locale: String) = {
    checkLocale(locale)
    data.prompts.get(foodCode) match {
      case Some(seq) => Right(seq)
      case None => Left(UndefinedCode)
    }
  }

  def brandNames(foodCode: String, locale: String) = {
    checkLocale(locale)
    data.brandNamesMap.get(foodCode) match {
      case Some(map) => Right(map)
      case None => Left(UndefinedCode)
    }
  }
  
}