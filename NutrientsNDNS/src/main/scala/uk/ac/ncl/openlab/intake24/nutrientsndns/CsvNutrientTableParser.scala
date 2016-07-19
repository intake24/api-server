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
import uk.ac.ncl.openlab.intake24.nutrients._
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._

object CsvNutrientTableParser {
  import Nutrient._

  val log = LoggerFactory.getLogger(CsvNutrientTableParser.getClass)

  def excelColumnToOffset(colRef: String) = {
    def r(s: String) = s.foldRight((0, 1)) {
      case (ch, (acc, mul)) => (acc + (ch - 'A' + 1) * mul, mul * 26)
    }
    r(colRef)._1
  }

  def parseTable(fileName: String, rowOffset: Int, idColumn: Int, tableMapping: Nutrient => Option[Int]): NutrientTable = {
    val rows = new CSVReader(new FileReader(fileName)).readAll().toSeq.map(_.toIndexedSeq)

    def readRow(row: IndexedSeq[String], rowIndex: Int): Map[Nutrient, Double] = Nutrient.types.foldLeft(Map[Nutrient, Double]()) {
      (acc, n) =>
        {
          try {
            tableMapping(n) match {
              case Some(colNum) => acc + (n -> row(colNum - 1).toDouble)
              case None => acc
            }
          } catch {
            case e: Throwable => {
              log.warn("Failed to read " + n.toString + " in row " + rowIndex + ", assuming data N/A")
              acc
            }
          }
        }
    }

    rows.zipWithIndex.drop(rowOffset).foldLeft(Map[String, Map[Nutrient, Double]]()) {
      case (map, row) => {
        val (rowSeq, rowIndex) = row

        map + (rowSeq(idColumn) -> readRow(rowSeq, rowIndex))
      }
    }
  }
}
