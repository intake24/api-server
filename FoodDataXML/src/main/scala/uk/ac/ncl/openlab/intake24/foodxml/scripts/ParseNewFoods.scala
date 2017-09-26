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
import scala.collection.JavaConverters._
import au.com.bytecode.opencsv.CSVReader
import uk.ac.ncl.openlab.intake24.foodxml.Util

object ParseNewFoods {

  case class Row(scranCode: String, description: String, ndnsCode: Int)

  def main(args: Array[String]): Unit = {
    val sourcePath = "D:\\SCRAN24\\Notes\\new_foods.csv"

    val rows = new CSVReader(new FileReader(sourcePath)).readAll().asScala.map(_.toSeq)

    val parsedRows = rows.tail.map(r => Row(r(0), r(1), r(2).toInt))

    val xml = <foods>
                {
                  parsedRows.map(row => {
                    <food code={row.scranCode} description={row.description} ndnsCode={row.ndnsCode.toString}/>
                  })
                }
              </foods>
    
   
    
    Util.writeXml(xml, "D:\\SCRAN24\\Data\\foods-new.xml")
   
  }
}