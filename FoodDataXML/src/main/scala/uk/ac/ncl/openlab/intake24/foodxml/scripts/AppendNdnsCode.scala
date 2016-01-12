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
import uk.ac.ncl.openlab.intake24.foodxml.FoodDefOld
import uk.ac.ncl.openlab.intake24.foodxml.Util

object AppendNdnsCode {

  def main(args: Array[String]): Unit = {
    val scranToNdns = scala.io.Source.fromFile("D:\\SCRAN24\\ExtractedDB\\ndns_scran_code_correspondence.txt").getLines.toList.map(s => {
      val Array(scranCode, ndnsCode) = s.split("\\s+")
      (scranCode, ndnsCode.toInt)
    }).toMap

    val foods = FoodDefOld.parseXml(XML.load("D:\\SCRAN24\\Data\\foods.xml"))

    FoodDefOld.writeXml("D:\\scratch\\foods-with-ndns-codes.xml", Util.mapFoods(foods, f => {
      val code = scranToNdns.get(f.code).getOrElse(-1)
      f.copy(ndnsCode = code)
    }))
  }
}