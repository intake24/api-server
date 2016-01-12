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

import scala.concurrent.Future
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.Prompt
import net.scran24.survey.Food
import net.scran24.fooddef.CategoryHeader
import net.scran24.fooddef.CategoryContents
import net.scran24.fooddef.FoodData
import net.scran24.fooddef.Category

trait FoodDataServiceAsync {
  def rootCategories(): Future[Seq[CategoryHeader]]

  def categoryContents(code: String): Future[CategoryContents]

  def foodDef(code: String): Future[Food]

  def foodData(code: String): Future[FoodData]

  def foodParentCategories(code: String): Future[Seq[String]]

  def foodAllCategories(code: String): Future[Seq[String]]

  def categoryParentCategories(code: String): Future[Seq[String]]

  def categoryAllCategories(code: String): Future[Seq[String]]

  def categoryDef(code: String): Future[Category]

  def asServedDef(id: String): Future[AsServedSet]

  def guideDef(id: String): Future[GuideImage]

  def drinkwareDef(id: String): Future[DrinkwareSet]

  def associatedFoodPrompts(foodCode: String): Future[Seq[Prompt]]

  def brandNames(foodCode: String): Future[Seq[String]]  
}