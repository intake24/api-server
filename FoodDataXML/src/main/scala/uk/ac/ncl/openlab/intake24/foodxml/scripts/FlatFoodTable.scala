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

import au.com.bytecode.opencsv.CSVWriter
import uk.ac.ncl.openlab.intake24.foodxml.{FoodDefOld, Util}

import scala.collection.JavaConverters._
import scala.xml.XML

object FlatFoodTable {

  def main(args: Array[String]): Unit = {
    val foods = FoodDefOld.parseXml(XML.load("D:\\SCRAN24\\Data\\foods.xml"))
    val writer = new CSVWriter(new FileWriter("D:\\scratch\\scran24_flat_foods.csv"))

    val lines = Util.flatten(foods, identity).sortBy(_.code).map(f => Array(f.code, f.description, f.ndnsCode.toString))

    writer.writeAll(lines.asJava)

    writer.close
  }
}