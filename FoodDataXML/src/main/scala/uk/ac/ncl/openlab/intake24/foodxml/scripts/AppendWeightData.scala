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

object AppendWeightData {
 def main(args: Array[String]): Unit = {
   /* 
    * No longer applicable, retained for reference
    * 
    * val sourcePath = "D:\\SCRAN24\\ExtractedDB\\tblPhoto_Weights.txt"
    val asServedPath = "D:\\SCRAN24\\Data\\as-served.xml"
    val asServedDestPath = "D:\\SCRAN24\\Data\\as-served-weight.xml"
      
    val asServedDef = AsServedDef.parseXml(XML.load(asServedPath))

    // header: "description","?","photo_id","weight"
    val rows = new CSVReader(new FileReader(sourcePath)).readAll().toList.map(_.toSeq)
    
    val weightTable = rows.map ( r => (r(2), r(3).toDouble)).toMap
    
    def appendWeight (s: AsServedImage) = weightTable.get(s.name) match {
      case Some(w) => s.copy (weight = w)
      case None => { println ("No weight data for " + s.name); s}
    }
    
    val newDef = asServedDef.values.toSeq.map(s => AsServedSet(s.id, s.description, s.portionSize.map(appendWeight(_)), s.leftovers.map(appendWeight(_)))) 
            
    AsServedDef.writeXml(asServedDestPath, newDef.sortBy(_.id)) */
  }
  
}