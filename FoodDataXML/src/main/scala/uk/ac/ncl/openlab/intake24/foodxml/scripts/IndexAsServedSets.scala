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
import Map._


object IndexAsServedSets {
  def main (args: Array[String]) = {
    
    /* No longer applicable
    
    val inFoods = "D:\\SCRAN24\\Data\\foods.xml"
    val inSets = "D:\\SCRAN24\\Data\\as-served.xml"
    val out = "D:\\SCRAN24\\Data\\foods-indexed.xml"
    
    val rootCat = FoodDef.parseXml(XML.load(inFoods))
    val sets = AsServedDef.parseXml(XML.load(inSets)).values
        
    def index (food: Food) = food.portionSize match {
      case as:AsServed => sets.find( set => set.portionSize == as.portionSize && set.leftovers == as.leftovers) match {
        case Some(ref) => food.copy (portionSize = AsServedRef(ref.id))
        case None => throw new RuntimeException ("Cannot find as-served set for: " + food.description)
      }
      case _ => food
    }
     
    FoodDef.writeXml(out, Util.mapFoods(rootCat, index)) */
  } 
}