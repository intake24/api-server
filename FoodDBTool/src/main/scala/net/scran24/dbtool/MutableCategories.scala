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

import uk.ac.ncl.openlab.intake24.CategoryV2
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.foodxml.XmlCategoryRecord
import uk.ac.ncl.openlab.intake24.foodxml.XmlFoodRecord

class DataException(message: String) extends Throwable

class MutableCategories(categories: Seq[XmlCategoryRecord]) {
  var map = categories.map(cat => (cat.code, cat)).toMap

  def find(code: String): Option[XmlCategoryRecord] = map.get(code)

  def create(category: XmlCategoryRecord) =
    if (map.contains(category.code)) throw new DataException(s"Code ${category.code} is already used")
    else map = map + (category.code -> category)

  def delete(code: String) =
    map = map - code

  def update(code: String, category: XmlCategoryRecord) =
    map = map + (code -> category)

  def foodSuperCategories(code: String): Seq[XmlCategoryRecord] =
    map.values.filter(cat => (cat.foods.exists(_ == code))).toSeq.sortBy(_.description)
    
  def categorySuperCategories(code: String): Seq[XmlCategoryRecord] =
    map.values.filter(cat => (cat.subcategories.exists(_ == code))).toSeq.sortBy(_.description)

  def isRoot(cat: XmlCategoryRecord) = !categorySuperCategories(cat.code).exists(!_.isHidden)
    
  def rootCategories: Seq[XmlCategoryRecord] =
    map.values.filter(isRoot).toSeq.sortBy(_.description)

  def uncategorisedFoods(foods: Seq[XmlFoodRecord]) =
    foods.filter(f => foodSuperCategories(f.code).isEmpty)

  def snapshot(): Seq[XmlCategoryRecord] =
    map.values.toSeq.sortBy(_.description)

  def tempcode() = {
    def mkCode(counter: Int) = "C%03d".format(counter)

    def rec(counter: Int): String = {
      if (counter == 999)
        throw new RuntimeException("Someone has exceeded 999 temporary category entries... o_Oa")
      val code = mkCode(counter)
      if (find(code).isDefined)
        rec(counter + 1)
      else
        code
    }

    rec(0)
  }
}