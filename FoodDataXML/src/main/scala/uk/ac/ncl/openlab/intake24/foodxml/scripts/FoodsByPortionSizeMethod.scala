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

import java.io.File
import java.io.FileWriter

import scala.xml.XML

import au.com.bytecode.opencsv.CSVWriter
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.foodxml.AsServedDef
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.foodxml.GuideImageDef

object FoodsByPortionSizeMethod extends App {

  val foods = FoodDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/foods.xml"))

  val asServed = scala.collection.mutable.Map[String, Seq[FoodRecord]]().withDefaultValue(Seq())
  val guide = scala.collection.mutable.Map[String, Seq[FoodRecord]]().withDefaultValue(Seq())

  val asServedSets = AsServedDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/as-served.xml"))
  val guideDef = GuideImageDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/guide.xml"))

  foods.foreach {
    food =>
      food.local.portionSize.foreach {
        method =>
          method.method match {
            case "as-served" =>
              method.parameters.find(_.name == "serving-image-set").foreach {
                case PortionSizeMethodParameter(_, value) => asServed += (value -> (asServed(value) :+ food))
              }
              method.parameters.find(_.name == "leftovers-image-set").foreach {
                case PortionSizeMethodParameter(_, value) => asServed += (value -> (asServed(value) :+ food))
              }

            case "guide-image" => 
              method.parameters.find(_.name == "guide-image-id").foreach {
                case PortionSizeMethodParameter(_, value) => guide += (value -> (guide(value) :+ food))
              }
            
            case _ => ()

          }

      }
  }
  
  val destPath = "/home/ivan/tmp/intake24/as_served.csv"

  val writer = new CSVWriter(new FileWriter(new File(destPath)))

  writer.writeNext(Array("As served set ID", "As served set description", "Food code", "Food description"))

  asServed.keySet.toSeq.sorted.foreach {
    set =>
      asServed(set).foreach {
        food =>
          writer.writeNext(Array(asServedSets(set).id, asServedSets(set).description, food.main.code, food.main.englishDescription))
      }
  }

  writer.close()
  
  val destPath2 = "/home/ivan/tmp/intake24/guide.csv"

  val writer2 = new CSVWriter(new FileWriter(new File(destPath2)))

  writer2.writeNext(Array("Guide image ID", "Guide image description", "Food code", "Food description"))

  guide.keySet.toSeq.sorted.foreach {
    id =>
      guide(id).foreach {
        food =>
          writer2.writeNext(Array(guideDef(id).id, guideDef(id).description, food.main.code, food.main.englishDescription))
      }
  }

  writer2.close()
  
    
  

}