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

import net.scran24.fooddef.CategoryV2
import org.slf4j.LoggerFactory

case class Categories(categories: Seq[CategoryV2]) {
  val log = LoggerFactory.getLogger(classOf[Categories])
  
  val categoryMap = categories.map(c => (c.code, c)).toMap
  
  val foodSuperCategories = categories.foldLeft(Map[String, Seq[String]]().withDefaultValue(Seq[String]())) {
    case (sup, cat) => cat.foods.foldLeft(sup) { case (sup, next) => sup + (next -> (sup(next) :+ cat.code)) }
  }.mapValues(_.sorted).withDefaultValue(Seq[String]())

  val categorySuperCategories = categories.foldLeft(Map[String, Seq[String]]().withDefaultValue(Seq[String]())) {
    case (sup, cat) => cat.subcategories.foldLeft(sup) { case (sup, next) => sup + (next -> (sup(next) :+ cat.code)) }
  }.mapValues(_.sorted).withDefaultValue(Seq[String]())

  val rootCategories = categories.filter(cat => !categorySuperCategories(cat.code).exists(!categoryMap(_).isHidden)).sortBy(_.description)

  // returns all categories containing this code,
  // not only the direct super categories but
  // also super super categories etc.

  def foodAllCategories(code: String): Seq[String] = {
    val sup = foodSuperCategories(code)    
    sup ++ sup.flatMap(categoryAllCategories(_))
  }
  
  def categoryAllCategories(code: String): Seq[String] = {
    val sup = categorySuperCategories(code)    
    sup ++ sup.flatMap(categoryAllCategories(_))
  }
 
  def find(code: String) = categoryMap.get(code) match {
    case Some(cat) => cat
    case None => throw new IllegalArgumentException(s"category with code $code is undefined")
  }
}