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

import java.io.FileWriter
import scala.collection.JavaConversions.seqAsJavaList
import scala.xml.XML
import au.com.bytecode.opencsv.CSVWriter
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import java.io.File
import scala.collection.JavaConversions._
import uk.ac.ncl.openlab.intake24.foodxml.FoodGroupDef
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef

case class NutrientTableRecord ( name: String, category: String )

object FoodRecodingForNZ {

  def buildNutritionTableIndex() = {
    val reader = new CSVReader(new FileReader(new File("/home/ivan/Projects/Intake24/intake24-data/nutrients.csv")))
    val table = reader.readAll().toSeq
    reader.close()
    
    val header = table(0).zipWithIndex.toMap
    
    val codeIndex = header("Food Code")
    val nameIndex = header("Name")
    val catIndex = header("Food Category")
    
    table.drop(1).map( row => (row(codeIndex), NutrientTableRecord(row(nameIndex), row(catIndex)))).toMap    
  }
  
  def main(args: Array[String]): Unit = {
    val foods = FoodDef.parseXml(XML.loadFile("/home/ivan/Projects/Intake24/intake24-data/foods.xml")).sortBy(_.code)
    val groups = FoodGroupDef.parseXml(XML.loadFile("/home/ivan/Projects/Intake24/intake24-data/food-groups.xml")).map {
      g => 
        (g.id, g.englishDescription)
    }.toMap
    val ndnsData = buildNutritionTableIndex
    val writer = new CSVWriter(new FileWriter("/home/ivan/Projects/Intake24/recoding/recoding.csv"))
    
    writer.writeNext(Array("Intake24 food code", "Intake24 description", "Intake24 food group", "NDNS code", "NDNS food name", "NDNS category"))

    writer.writeAll(foods.map(food => {
      
      if (!food.localData.nutrientTableCodes.contains("NDNS") ) {
        println ("No ndns code for " + food.code)
        Array(food.code, food.englishDescription, groups(food.groupCode), food.localData.nutrientTableCodes("NDNS"), "N/A", "N/A")
      } else if (!ndnsData.contains(food.localData.nutrientTableCodes("NDNS"))) {
        println (food.code + " " + food.englishDescription + " " + food.localData.nutrientTableCodes("NDNS"))
        Array(food.code, food.englishDescription, groups(food.groupCode), food.localData.nutrientTableCodes("NDNS"), "N/A", "N/A")
      }
      else {
        val ndnsRecord = ndnsData(food.localData.nutrientTableCodes("NDNS"))
        Array(food.code, food.englishDescription, groups(food.groupCode), food.localData.nutrientTableCodes("NDNS"), ndnsRecord.name, ndnsRecord.category)
      }
    }))
    

    writer.close
  }

}