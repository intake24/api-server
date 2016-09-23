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

import scala.xml.XML
import uk.ac.ncl.openlab.intake24.foodxml.AsServedDef
import java.io.File
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef

object DefaultPortionSizeSelectionImages extends App {

  val foods = FoodDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/foods.xml"))

  val map = foods.foldLeft(Map[String, Set[String]]()) {
    (map, food) =>
      food.portionSizeMethods.foldLeft(map) {
        (map, psm) =>
          map + (psm.description -> (map.getOrElse(psm.description, Set()) + psm.imageUrl))
      }
  }

  println("{")
  
  map.keySet.toSeq.sorted.foreach {
    key =>
      val arr = map(key).map(url => "\"" + url + "\"").mkString("[", ", ", "]")
      println (s"""  "$key" : $arr,""")
  }
  
  println("}")

}
