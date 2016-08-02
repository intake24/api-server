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
import scala.collection.JavaConversions.seqAsJavaList
import au.com.bytecode.opencsv.CSVReader
import uk.ac.ncl.openlab.intake24.AssociatedFoodV1
import scala.xml.XML
import uk.ac.ncl.openlab.intake24.foodxml.CategoryDef
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.foodxml.Util

object ParseAssociatedFoods {
  def main(args: Array[String]): Unit = {
    val sourcePath = "/home/ivan/Projects/Intake24/Misc/prompts.csv"

    // short_code | is main | associated food code | prompt text ...																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									

    val rows = new CSVReader(new FileReader(sourcePath)).readAll().toList.map(_.toSeq)

    val foods = FoodDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/foods.xml"))
    val cats = CategoryDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data/categories.xml"))

    val prompts = rows.map(_.filterNot(_.isEmpty)).filter(_.length > 1).tail.groupBy(_(0)).mapValues(rows =>
      {
        val pdefs = rows.map( row => AssociatedFoodV1(row(2), row(3), row(1).toBoolean, row(4)))

        pdefs.foreach(p => {
          if (!(cats.exists(_.code == p.category) || foods.exists(_.main.code == p.category)))
            println("Undefined category or food " + p.category + " in row \n" + rows)
        })
        
        pdefs
      })

    val xml = <scran24-food-prompts>
                {
                  prompts.map(p =>
                    <food code={ p._1 }>
                      { p._2.map(q => <prompt category={ q.category } text={ q.promptText } linkAsMain={ q.linkAsMain.toString } genericName={ q.genericName }/>) }
                    </food>)
                }
              </scran24-food-prompts>

    Util.writeXml(xml, "/home/ivan/Projects/Intake24/intake24-data/new-prompts.xml")
  }
}