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

import java.io.{File, FileReader, FileWriter}

import au.com.bytecode.opencsv.{CSVReader, CSVWriter}
import uk.ac.ncl.openlab.intake24.foodxml.{FoodDef, FoodGroupDef}

import scala.collection.JavaConverters._
import scala.xml.XML

case class NutrientTableRecord(name: String, category: String)

object FoodRecodingForNZ {

  def buildNutritionTableIndex() = {
    val reader = new CSVReader(new FileReader(new File("/home/ivan/Projects/Intake24/intake24-data/nutrients.csv")))
    val table = reader.readAll().asScala
    reader.close()

    val header = table(0).zipWithIndex.toMap

    val codeIndex = header("Food Code")
    val nameIndex = header("Name")
    val catIndex = header("Food Category")

    table.drop(1).map(row => (row(codeIndex), NutrientTableRecord(row(nameIndex), row(catIndex)))).toMap
  }

  def main(args: Array[String]): Unit = {
    val foods = FoodDef.parseXml(XML.loadFile("/home/ivan/Projects/Intake24/intake24-data/foods.xml")).sortBy(_.code)
    val groups = FoodGroupDef.parseXml(XML.loadFile("/home/ivan/Projects/Intake24/intake24-data/food-groups.xml")).map {
      g =>
        (g.id, g.description)
    }.toMap
    val ndnsData = buildNutritionTableIndex
    val writer = new CSVWriter(new FileWriter("/home/ivan/Projects/Intake24/recoding/recoding.csv"))

    writer.writeNext(Array("Intake24 food code", "Intake24 description", "Intake24 food group", "NDNS code", "NDNS food name", "NDNS category"))

    writer.writeAll((foods.map(food => {

      if (!food.nutrientTableCodes.contains("NDNS")) {
        println("No ndns code for " + food.code)
        Array(food.code, food.description, groups(food.groupCode), food.nutrientTableCodes("NDNS"), "N/A", "N/A")
      } else if (!ndnsData.contains(food.nutrientTableCodes("NDNS"))) {
        println(food.code + " " + food.description + " " + food.nutrientTableCodes("NDNS"))
        Array(food.code, food.description, groups(food.groupCode), food.nutrientTableCodes("NDNS"), "N/A", "N/A")
      }
      else {
        val ndnsRecord = ndnsData(food.nutrientTableCodes("NDNS"))
        Array(food.code, food.description, groups(food.groupCode), food.nutrientTableCodes("NDNS"), ndnsRecord.name, ndnsRecord.category)
      }
    })).asJava)

    writer.close
  }
}
