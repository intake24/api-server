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

import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import scala.collection.immutable.ListSet
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.foodxml.XmlCategoryRecord

case class PortionSizeResolver(foods: MutableFoods, categories: MutableCategories) {

  def resolveInheritance(code: String): Seq[XmlCategoryRecord] = {
    def rec(q: Seq[String], result: Seq[XmlCategoryRecord], visited: Set[String]): Seq[XmlCategoryRecord] =
      q match {
        case Nil => result.toSeq
        case x :: xs => {
          val supercats = categories.categorySuperCategories(x).filter(c => !visited.contains(c.code))
          val codes = supercats.map(_.code)
          rec(codes ++ xs, result ++ supercats, visited ++ codes)
        }
      }
    rec(Seq(code), Seq(), Set())
  }

  def categoryInheritedAttributes(code: String) = resolveInheritance(code).map(_.attributes)

  def foodInheritedAttributes(code: String) = {
    val parentCats = categories.foodSuperCategories(code)
    (parentCats ++ parentCats.flatMap(c => resolveInheritance(c.code))).map(_.attributes)
  }

  def categoryInheritedPortionSize(code: String) = {
    resolveInheritance(code).find(_.portionSizeMethods.nonEmpty) match {
      case Some(cat) => cat.portionSizeMethods
      case None => Seq()
    }
  }

  def foodInheritedPortionSize(code: String) = {
    val parentCats = categories.foodSuperCategories(code)
    (parentCats ++ parentCats.flatMap(c => resolveInheritance(c.code))).find(_.portionSizeMethods.nonEmpty) match {
      case Some(cat) => cat.portionSizeMethods
      case None => Seq()
    }
  }

  def foodInheritedAttribute[T](code: String, get: InheritableAttributes => Option[T]) =
    foodInheritedAttributes(code).map(get).find(_.isDefined).flatten

  def categoryInheritedAttribute[T](code: String, get: InheritableAttributes => Option[T]) =
    categoryInheritedAttributes(code).map(get).find(_.isDefined).flatten
}
