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
import uk.ac.ncl.openlab.intake24.foodxml.FoodDefOld
import uk.ac.ncl.openlab.intake24.foodxml.Util

object MarkDrinks {
  def main (args: Array[String]) = {
    
    val inFoods = "D:\\SCRAN24\\Data\\foods.xml"
    val inDrinks= "D:\\SCRAN24\\ExtractedDB\\drinks.txt"
    val out = "D:\\SCRAN24\\Data\\foods-drinks.xml"
    
    val rootCat = FoodDefOld.parseXml(XML.load(inFoods))
    val drinks = scala.io.Source.fromFile(inDrinks).getLines.toSet
     
    FoodDefOld.writeXml(out, Util.mapFoods(rootCat, f => f.copy (isDrink = drinks.contains (f.code))))
  }
}