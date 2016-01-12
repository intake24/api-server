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

package uk.ac.ncl.openlab.intake24.nutrientsndns

import org.slf4j.LoggerFactory
import net.scran24.fooddef.nutrients._
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._

object NutrientDef {
  import Nutrient._
  
  val log = LoggerFactory.getLogger(NutrientDef.getClass)

  val tableMapping: Map[Nutrient, Int] =
    Map(
      (Protein, 20),
      (Fat, 22),
      (Carbohydrate, 24),
      (EnergyKcal, 26),
      (EnergyKj, 28),
      (Alcohol, 30),
      (TotalSugars, 38),
      (Nmes, 40),
      (SatdFa, 56),
      (Cholesterol, 66),
      (VitaminA, 78),
      (VitaminD, 80),
      (VitaminC, 92),
      (VitaminE, 94),
      (Folate, 100),
      (Sodium, 106),
      (Calcium, 110),
      (Iron, 116),
      (Zinc, 124),
      (Selenium, 132))
      
  val zeroData = Nutrient.list.map (n => (n.key, 0.0)).toMap

  def parseTable(fileName: String): NutrientData = {
    val rows = new CSVReader(new FileReader(fileName)).readAll().toSeq.map(_.toIndexedSeq)

    val descriptions: Map[String, String] = Nutrient.list.map(n => (n.key, rows.head(tableMapping(n) - 1))).toMap

    def readRow(row: IndexedSeq[String], rowIndex: Int): Map[String, java.lang.Double] = Nutrient.list.map(n => {
      try {
        val v: java.lang.Double = row(tableMapping(n) - 1).toDouble
        (n.key, v)
      } catch {
        case e: Throwable => {
          log.warn("Failed to read " + n.toString + " in row " + rowIndex + ", assuming 0")
          (n.key, 0.0: java.lang.Double)
        }
      }
    }).toMap

    val values = rows.zipWithIndex.tail.foldLeft(Map[Int, Map[String, java.lang.Double]]()) {
      case (map, row) => {
        val (rowSeq, rowIndex) = row
        
        map + (rowSeq(0).toInt -> readRow(rowSeq, rowIndex))
      }
    }

    NutrientData(descriptions, values)
  }
}