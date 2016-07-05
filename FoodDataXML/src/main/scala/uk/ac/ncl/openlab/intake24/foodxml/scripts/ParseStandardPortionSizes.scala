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
import uk.ac.ncl.openlab.intake24.PortionSizeMethod



object ParseStandardPortionSizes {
/*  case class Row(code: String, unitName: String, omitFoodDesc: Boolean, weight: Double)
  
  def main(args: Array[String]): Unit = {
    val sourcePath = "D:\\SCRAN24\\Notes\\standard_portion.csv"
    val foodsSrc = "D:\\SCRAN24\\Data\\foods.xml"
    val foodsDst = "D:\\SCRAN24\\Data\\foods-stdportion.xml"

    //   0              1               2            3         4               5             6
    //	Short Code,Long description,portion/item,unit name,plural unit name,omit food name,Weight (g),Is there a Photo?,																		
    val rows = new CSVReader(new FileReader(sourcePath)).readAll().toSeq.map(_.toSeq)

    val parsedRows = rows.tail.map(r => Row(r(0), r(4), r(5) == "Y", r(6).toDouble))
    
    val foods = FoodDef.parseXml(XML.load(foodsSrc)).map(f => (f.code, f)).toMap
    
    val grouped = parsedRows.groupBy(_.code) 
    
    val newFoods = grouped.keySet.foldLeft(foods) { case (foods, code) => {
        foods.get(code) match {
          case Some(food) => {
            println { "Modifying " + code}
            val units = StandardUnitDef.toPortionSizeParameters(grouped(code).map(row => StandardUnitDef(row.unitName, row.weight, row.omitFoodDesc)).sortBy(_.weight))
            foods + (code -> food.copy (portionSize = food.portionSize :+ PortionSizeMethod("standard-portion", "No description", "portion/placeholder.jpg", units)))
          }
          case None => { println ("Missing food: " + code); foods } 
        }
    }}
    
    Util.writeXml(FoodDef.toXml(newFoods.values.toSeq.sortBy(_.description)), foodsDst)
  }*/
}