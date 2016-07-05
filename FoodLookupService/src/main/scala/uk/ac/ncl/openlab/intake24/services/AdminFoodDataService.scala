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

import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.CategoryRecord
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.CategoryContents
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.DrinkwareHeader
import uk.ac.ncl.openlab.intake24.FoodGroup
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.GuideHeader
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.NutrientTable

sealed trait UpdateResult 

case object Success extends UpdateResult

case object VersionConflict extends UpdateResult

case class InvalidRequest(errorCode: String, message: String) extends UpdateResult

case class SqlException(message: String) extends UpdateResult

case class NewFood(code: String, englishDescription: String, groupCode: Int, attributes: InheritableAttributes)

case class NewFoodAutoCode(englishDescription: String, groupCode: Int, attributes: InheritableAttributes)

case class NewCategory(code: String, englishDescription: String, isHidden: Boolean, attributes: InheritableAttributes)

trait AdminFoodDataService {
  
  // Read
   
  def uncategorisedFoods(locale: String): Seq[FoodHeader]
  
  def rootCategories(locale: String): Seq[CategoryHeader]

  def categoryContents(code: String, locale: String): CategoryContents

  def foodRecord(code: String, locale: String): Either[CodeError, FoodRecord]
 
  def isCategoryCode(code: String): Boolean
  
  def isFoodCode(code: String): Boolean

  def foodParentCategories(code: String, locale: String): Seq[CategoryHeader]

  def foodAllCategories(code: String, locale: String): Seq[CategoryHeader]

  // Categories are alphabetically sorted
  def categoryParentCategories(code: String, locale: String): Seq[CategoryHeader]

  def categoryAllCategories(code: String, locale: String): Seq[CategoryHeader]

  def categoryRecord(code: String, locale: String): Either[CodeError, CategoryRecord]
  
  def allAsServedSets(): Seq[AsServedHeader]
 
  def allGuideImages(): Seq[GuideHeader]
  
  def allDrinkware(): Seq[DrinkwareHeader]
  
  def allFoodGroups(locale: String): Seq[FoodGroup]
  
  def foodGroup(code: Int, locale: String): Option[FoodGroup]
   
  def nutrientTables(): Seq[NutrientTable]
  
  def searchFoods(searchTerm: String, locale: String): Seq[FoodHeader]
  
  def searchCategories(searchTerm: String, locale: String): Seq[CategoryHeader]
  
  // Write
  
  def updateFoodBase(foodCode: String, foodBase: MainFoodRecord): UpdateResult
  
  def updateFoodLocal(foodCode: String, locale: String, foodLocal: LocalFoodRecord): UpdateResult
  
  def isFoodCodeAvailable(code: String): Boolean
  
  def createFood(newFood: NewFood): UpdateResult
  
  def createFoodWithTempCode(newFood: NewFood): Either[InvalidRequest, String]
  
  def deleteFood(foodCode: String): UpdateResult
  
  def updateCategoryBase(categoryCode: String, categoryBase: MainCategoryRecord): UpdateResult
  
  def updateCategoryLocal(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): UpdateResult
  
  def isCategoryCodeAvailable(code: String): Boolean
  
  def createCategory(newCategory: NewCategory): UpdateResult
  
  def deleteCategory(categoryCode: String): UpdateResult
  
  def addFoodToCategory(categoryCode: String, foodCode: String): UpdateResult
  
  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): UpdateResult
  
  def removeFoodFromCategory(categoryCode: String, foodCode: String): UpdateResult
  
  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): UpdateResult
  
  def updateAssociatedFoods(foodCode: String, locale: String, associatedFoods: Seq[AssociatedFood]): UpdateResult
}
