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

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.CategoryV2
import Util._
import uk.ac.ncl.openlab.intake24.foodxml.XmlFoodRecord
import uk.ac.ncl.openlab.intake24.foodxml.XmlCategoryRecord

class ProblemChecker(foods: MutableFoods, categories: MutableCategories, portionSize: PortionSizeResolver) {

  def checkMultipleChoice(food: XmlFoodRecord) = {
    if (food.portionSizeMethods.size > 1)
      food.portionSizeMethods.map(p => p.description.toLowerCase() == "no description" || p.imageUrl.toLowerCase() == "portion/placeholder.jpg").exists(x => x)
    else false
  }
  
  def checkDescriptionInRecipe(food: XmlFoodRecord) = {
    if (food.portionSizeMethods.exists(_.useForRecipes))
      food.portionSizeMethods.map(p => p.description.toLowerCase() == "no description" || p.imageUrl.toLowerCase() == "portion/placeholder.jpg").exists(x => x)
    else false
  }

  def problems(food: XmlFoodRecord): Seq[String] = Seq(
    conditional(food.nutrientTableCodes.isEmpty, "\"" + food.description + "\" does not have a nutrient table code assigned"),
    conditional(food.groupCode == 0 || food.groupCode == -1, "\"" + food.description + "\" is not assigned to a food group"),
    conditional(categories.foodSuperCategories(food.code).isEmpty, "\"" + food.description + "\" is not assigned to any category"),
    conditional(food.portionSizeMethods.isEmpty && portionSize.foodInheritedPortionSize(food.code).isEmpty, "Portion size method cannot be resolved for \"" + food.description + "\""),
    conditional(checkMultipleChoice(food), "No description or image for multiple-choice portion estimation for \"" + food.description + "\""),
    conditional(checkDescriptionInRecipe(food), "No description or image for portion estimation for \"" + food.description + "\" (description and image required because it can be used as a recipe ingredient)")).flatten

  def subProblems(category: XmlCategoryRecord): Seq[String] = {
    val (missingFoods, okFoods) = category.foods.partition(foods.find(_).isEmpty)
    val (missingCategories, subcategories) = category.subcategories.partition(categories.find(_).isEmpty)

    missingFoods.map("Food code " + _ + " listed in \"" + category.description + "\" is undefined") ++
      missingCategories.map("CategoryV2 code " + _ + " listed in \"" + category.description + " is undefined") ++
      okFoods.map(foods.find(_).get).flatMap(problems(_)) ++
      subcategories.map(categories.find(_).get).flatMap(problems(_))
  }

  def problems(category: XmlCategoryRecord) = Seq(
    conditional((category.subcategories.size + category.foods.size) == 0, "\"" + category.description + "\" is an empty category, consider deleting"),
    conditional((category.subcategories.size + category.foods.size) == 1, "\"" + category.description + "\" contains only a single item, consider merging with parent category")).flatten ++ subProblems(category)
}