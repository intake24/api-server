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

import net.scran24.fooddef.Food
import java.util.UUID
import net.scran24.fooddef.FoodBase
import net.scran24.fooddef.FoodLocal
import net.scran24.fooddef.CategoryBase
import net.scran24.fooddef.CategoryLocal
import net.scran24.fooddef.InheritableAttributes
import net.scran24.fooddef.InheritableAttributes

sealed trait UpdateResult 

case object Success extends UpdateResult

case object VersionConflict extends UpdateResult

case class InvalidRequest(errorCode: String, message: String) extends UpdateResult

case class SqlException(message: String) extends UpdateResult

case class NewFood(code: String, englishDescription: String, groupCode: Int, attributes: InheritableAttributes)

case class NewCategory(code: String, englishDescription: String, isHidden: Boolean, attributes: InheritableAttributes)

trait FoodDataEditingService {
  
  def updateFoodBase(foodCode: String, foodBase: FoodBase): UpdateResult
  
  def updateFoodLocal(foodCode: String, locale: String, foodLocal: FoodLocal): UpdateResult
  
  def isFoodCodeAvailable(code: String): Boolean
  
  def createFood(newFood: NewFood): UpdateResult
  
  def deleteFood(foodCode: String): UpdateResult
  
  def updateCategoryBase(categoryCode: String, categoryBase: CategoryBase): UpdateResult
  
  def updateCategoryLocal(categoryCode: String, locale: String, categoryLocal: CategoryLocal): UpdateResult
  
  def isCategoryCodeAvailable(code: String): Boolean
  
  def createCategory(newCategory: NewCategory): UpdateResult
  
  def deleteCategory(categoryCode: String): UpdateResult
  
  def addFoodToCategory(categoryCode: String, foodCode: String): UpdateResult
  
  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): UpdateResult
  
  def removeFoodFromCategory(categoryCode: String, foodCode: String): UpdateResult
  
  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): UpdateResult
}