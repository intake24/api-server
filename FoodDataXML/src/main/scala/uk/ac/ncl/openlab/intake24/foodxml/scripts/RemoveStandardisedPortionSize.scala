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

import java.io.FileReader
import scala.collection.JavaConversions.asScalaBuffer
import au.com.bytecode.opencsv.CSVReader
import scala.xml.XML
import uk.ac.ncl.openlab.intake24.PortionSizeMethod

object RemoveStandardisedPortionSize {

 /* def main(args: Array[String]): Unit = {
    val foodsXmlPath = "D:\\SCRAN24\\Data\\foods.xml"
    val dstPath = "D:\\SCRAN24\\Data\\foods-cleaned.xml"
    val sourcePath = "D:\\SCRAN24\\Notes\\keep_std.csv"

    val keepStdFor = new CSVReader(new FileReader(sourcePath)).readAll().toList.map(_.toSeq).tail.flatMap(r =>
      r.tail.filterNot(_.isEmpty())).toSet

    val foods = FoodDef.parseXml(XML.load(foodsXmlPath))

    val cleaned = foods.map(f => {
      
      def cleanBadImage(p: PortionSizeMethod) = if (!p.imageUrl.contains(".jpg")) p.copy(imageUrl = "portion/placeholder.jpg") else p
      
      val cf = f.copy(portionSize = f.portionSize.map(cleanBadImage))
      
      if (cf.portionSize.exists(_.name == "standard-portion") && cf.portionSize.length > 1) {
        println(cf.code)

        if (keepStdFor.contains(cf.code)) {
          println("Keeping")
          cf
        } else {
          println("Removing")
          cf.copy(portionSize = cf.portionSize.filterNot(_.name == "standard-portion"))
        }
      } else cf
    })
    
    Util.writeXml(FoodDef.toXml(cleaned), dstPath)
  }*/
}