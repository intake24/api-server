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

package uk.ac.ncl.openlab.intake24.foodxml.scripts

import uk.ac.ncl.openlab.intake24.foodxml.{Categories, CategoryDef, FoodDef}

import scala.xml.XML

object CheckCycles extends App {

  val foods = FoodDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/foods.xml"))
  val categories = Categories(CategoryDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/categories.xml")))

  println(foods.size)

  def findCycles(code: String) = {

    def r(cats: Seq[String], visited: Seq[String]): Option[(Seq[String], String)] = {
      cats.find(visited.contains(_)) match {
        case Some(cat) => Some(visited, cat)
        case None => {
          val supercats = cats.flatMap(categories.categorySuperCategories(_))
          if (supercats.isEmpty)
            None
          else
            r(supercats, visited ++ cats)
        }
      }
    }

    r(categories.categorySuperCategories(code), Seq())
  }

  foods.foreach {
    food =>
      findCycles(food.code) match {
        case Some(cats) => println("Cycle: " + food.code + " " + cats)
        case _ => ()
      }
  }

}
