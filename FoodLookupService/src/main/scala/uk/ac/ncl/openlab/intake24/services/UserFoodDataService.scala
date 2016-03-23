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

package uk.ac.ncl.openlab.intake24.services

import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.Prompt
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.Food
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.FoodHeader
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.CategoryContents
import net.scran24.fooddef.FoodData
import net.scran24.fooddef.SplitList
import net.scran24.fooddef.FoodGroup
import net.scran24.fooddef.Category
import net.scran24.fooddef.AsServedHeader
import net.scran24.fooddef.GuideHeader
import net.scran24.fooddef.DrinkwareHeader
import net.scran24.fooddef.AsServedHeader
import net.scran24.fooddef.NutrientTable
import net.scran24.fooddef.UserCategoryHeader
import net.scran24.fooddef.UserCategoryContents

sealed trait SourceRecord

object SourceRecord {
  case class FoodRecord(code: String) extends SourceRecord
  case class CategoryRecord(code: String) extends SourceRecord
}

sealed trait InheritableAttributeSource

object InheritableAttributeSource {
  case class FoodRecord(code: String) extends InheritableAttributeSource
  case class CategoryRecord(code: String) extends InheritableAttributeSource
  case object Default extends InheritableAttributeSource
}

sealed trait SourceLocale

object SourceLocale {
  case class Current(locale: String) extends SourceLocale
  case class Prototype(locale: String) extends SourceLocale
  case class Fallback(locale: String) extends SourceLocale
}

case class InheritableAttributeSources(sameAsBeforeOptionSource: InheritableAttributeSource, readyMealOptionSource: InheritableAttributeSource, reasonableAmountSource: InheritableAttributeSource)

case class FoodDataSources(localDescriptionSource: SourceLocale, nutrientTablesSource: SourceLocale, portionSizeSource: (SourceLocale, SourceRecord), inheritableAttributesSources: InheritableAttributeSources)

trait UserFoodDataService {
    
  def rootCategories(locale: String): Seq[UserCategoryHeader]

  def categoryContents(code: String, locale: String): Either[CodeError, UserCategoryContents]

  def foodData(code: String, locale: String): Either[FoodDataError, (FoodData, FoodDataSources)]
  
  def foodAllCategories(code: String): Seq[String]
  
  def categoryAllCategories(code: String): Seq[String]
  
  def isCategoryCode(code: String): Boolean
     
  def asServedDef(id: String): Either[ResourceError, AsServedSet]
  
  def guideDef(id: String): Either[ResourceError, GuideImage]
  
  def drinkwareDef(id: String): Either[ResourceError, DrinkwareSet]

  def associatedFoodPrompts(foodCode: String, locale: String): Either[CodeError, Seq[Prompt]]

  def brandNames(foodCode: String, locale: String): Either[CodeError, Seq[String]]
  
}
