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

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import scala.xml.XML
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.AsServedImage
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.foodxml.Util

object AppendFoodGroups {
  
  def main(args: Array[String]): Unit = {
    val sourcePath = "D:\\SCRAN24\\Notes\\food_group_codes.csv"

    // scran code | group code																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									

    val codes = new CSVReader(new FileReader(sourcePath)).readAll().toList.map(_.toSeq).tail.distinct.map ( row => (row(0), row(1).toInt)).toMap
    
    val foods = FoodDef.parseXml(XML.load("D:\\SCRAN24\\Data\\foods.xml")).map( food => {
      codes.get(food.code) match {
        case Some(code) => food.copy (groupCode = code)
        case None => {
          println (food.code)
          food
        }
      }      
    })
    
    Util.writeXml(FoodDef.toXml(foods), "D:\\SCRAN24\\Data\\foods-groups.xml")
  }
}