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
import uk.ac.ncl.openlab.intake24.AssociatedFood
import scala.xml.XML
import uk.ac.ncl.openlab.intake24.foodxml.Util

object ParseBrandNames {
  def main(args: Array[String]): Unit = {
    val sourcePath = "D:\\SCRAN24\\Notes\\brand_names.csv"

    // header
    // code | list of brands ...

    val rows = new CSVReader(new FileReader(sourcePath)).readAll().toList.tail.map(_.toSeq)

    val xml = <brand-names>
                {
                  rows.map(r =>
                    <food code={ r(0) }>
                      {
                        (r.tail.tail.filter(_.nonEmpty) :+ "I don't know") .map(n => <brand name={ n }/>)
                      }
                    </food>)
                }
              </brand-names>

    Util.writeXml(xml, "D:\\SCRAN24\\Data\\brands.xml")
  }
}