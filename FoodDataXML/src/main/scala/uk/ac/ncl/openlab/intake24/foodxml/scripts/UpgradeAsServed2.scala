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

import scala.xml.XML

object UpgradeAsServed2 {
  def main(args: Array[String]) = {
    /*val asServedPath = "D:\\SCRAN24\\Data\\as-served.xml"
    val asServedDestPath = "D:\\SCRAN24\\Data\\as-served-v3.xml"
      
    val asServed = AsServedDef.parseXml(XML.load(asServedPath))
    
    val z = Map[String, AsServedSetV2]()
    
    val asServedV2 = asServed.foldLeft(z){ case (map, (k, v)) => {
      map + (v.id -> AsServedSetV2(v.id, v.description, v.portionSize)) + ((v.id + "_leftovers") -> AsServedSetV2(v.id + "_leftovers", v.description + " (leftovers)", v.leftovers))
    }}
    
    AsServedDef.writeXmlV2(asServedDestPath, asServedV2.values)
    */
  }
}