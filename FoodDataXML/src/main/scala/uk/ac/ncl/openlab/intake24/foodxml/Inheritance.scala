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

import uk.ac.ncl.openlab.intake24.api.data.InheritableAttributes

case class Inheritance(categories: Categories) {

  def foodInheritedAttributes(foodCode: String) = categories.foodAllCategories(foodCode).map(code => categories.find(code).attributes)

  def categoryInheritedAttributes(categoryCode: String) = categories.categoryAllCategories(categoryCode).map(code => categories.find(code).attributes)

  def foodInheritedPortionSize(foodCode: String) = categories.foodAllCategories(foodCode).flatMap(code => categories.find(code).portionSizeMethods).distinct

  def categoryInheritedPortionSize(categoryCode: String) = categories.categoryAllCategories(categoryCode).flatMap(code => categories.find(code).portionSizeMethods).distinct

  def foodInheritedAttribute[T](foodCode: String, get: InheritableAttributes => Option[T]) =
    foodInheritedAttributes(foodCode).map(get).find(_.isDefined).flatten

  def categoryInheritedAttribute[T](categoryCode: String, get: InheritableAttributes => Option[T]) =
    categoryInheritedAttributes(categoryCode).map(get).find(_.isDefined).flatten
}
