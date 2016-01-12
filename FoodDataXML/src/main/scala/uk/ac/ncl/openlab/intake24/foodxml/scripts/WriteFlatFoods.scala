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

import java.io.File
import java.io.PrintWriter
import scala.xml.Attribute
import scala.xml.Null
import scala.xml.PrettyPrinter
import scala.xml.Text
import scala.xml.XML
import uk.ac.ncl.openlab.intake24.foodxml.FoodDefOld
import uk.ac.ncl.openlab.intake24.foodxml.Util

object WriteFlatFoods {
  def main(args: Array[String]) = {
    val inFoods = "D:\\SCRAN24\\Data\\foods.xml"
    val rootCat = FoodDefOld.parseXml(XML.load(inFoods))

    val x = Util.flatten(rootCat, identity)

    val xml =
      <foods>
        {
          x.sortBy(_.code).map(f => <food>
                                      {
                                        f.portionSize.map(s =>
                                          <portion-size>
                                            {
                                              s.parameters.map(p => <param/> % Attribute(None, "value", Text(p.value), Null)
                                                % Attribute(None, "name", Text(p.name), Null))
                                            }
                                          </portion-size> % Attribute(None, "method", Text(s.method), Null))
                                      }
                                    </food> %
            Attribute(None, "ndnsCode", Text(f.ndnsCode.toString), Null) %
            Attribute(None, "code", Text(f.code), Null) %
            Attribute(None, "isDrink", Text(if (f.isDrink) "true" else "false"), Null) %
            Attribute(None, "description", Text(f.description), Null))
        }
      </foods>

    Util.writeXml(xml, "D:\\SCRAN24\\Data\\foods-old.xml")
 }
}