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

trait AdminFoodDataService {
   
  def uncategorisedFoods(locale: String): Seq[FoodHeader]
  
  def rootCategories(locale: String): Seq[CategoryHeader]

  def categoryContents(code: String, locale: String): CategoryContents

  def foodDef(code: String, locale: String): Food
 
  def isCategoryCode(code: String): Boolean
  
  def isFoodCode(code: String): Boolean

  def foodParentCategories(code: String, locale: String): Seq[CategoryHeader]

  def foodAllCategories(code: String, locale: String): Seq[CategoryHeader]

  // Categories are alphabetically sorted
  def categoryParentCategories(code: String, locale: String): Seq[CategoryHeader]

  def categoryAllCategories(code: String, locale: String): Seq[CategoryHeader]

  def categoryDef(code: String, locale: String): Category
  
  def allAsServedSets(): Seq[AsServedHeader]
 
  def allGuideImages(): Seq[GuideHeader]
  
  def allDrinkware(): Seq[DrinkwareHeader]
  
  def allFoodGroups(locale: String): Seq[FoodGroup]
  
  def foodGroup(code: Int, locale: String): Option[FoodGroup]
   
  def nutrientTables(): Seq[NutrientTable]
  
  def searchFoods(searchTerm: String, locale: String): Seq[FoodHeader]
  
  def searchCategories(searchTerm: String, locale: String): Seq[CategoryHeader]
}
