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
import uk.ac.ncl.openlab.intake24.foodxml.CategoryDef

object FoodRecodingCategories extends App {

  val categories = CategoryDef.parseXml(XML.loadFile("/home/ivan/Projects/Intake24/intake24-data/categories.xml")).sortBy(_.code)
  val writer = new CSVWriter(new FileWriter("/home/ivan/Projects/Intake24/recoding/categories.csv"))

  writer.writeNext(Array("Intake24 category code", "English description"))

  writer.writeAll(categories.map {
    category => Array(category.code, category.description)
  })

  writer.close
}