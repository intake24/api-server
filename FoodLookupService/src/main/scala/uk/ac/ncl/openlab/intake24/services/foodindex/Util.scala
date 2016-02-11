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

package uk.ac.ncl.openlab.intake24.services.foodindex

import net.scran24.fooddef.Food
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.FoodHeader
import net.scran24.fooddef.CategoryV2
import net.scran24.fooddef.UserCategoryHeader
import net.scran24.fooddef.UserFoodHeader

sealed trait IndexEntry

case class CategoryEntry(header: UserCategoryHeader) extends IndexEntry
case class FoodEntry(header: UserFoodHeader) extends IndexEntry

object Util {
  def mkHeader(category: CategoryV2) = CategoryHeader(category.code, category.description, Some(category.description), category.isHidden)
  def mkHeader(food: Food) = FoodHeader(food.code, food.englishDescription, food.localData.localDescription)
}
