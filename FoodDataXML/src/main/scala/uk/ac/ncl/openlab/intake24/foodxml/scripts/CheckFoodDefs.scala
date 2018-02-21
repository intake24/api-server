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
import uk.ac.ncl.openlab.intake24.foodxml.{CategoryV1, FoodDefOld, Util}

object FindProblems {

  def findMissingDescriptions(root: Map[String, CategoryV1]) = Util.flatten(root, identity).filter(_.description == "???") 
    
  def findMissingPortionSize(root: Map[String, CategoryV1]) = Util.flatten(root, identity).filter(_.portionSize.isEmpty) 
 
  def main(args: Array[String]): Unit = {
    val inFoods = "D:\\SCRAN24\\Data\\foods.xml"
    val rootCat = FoodDefOld.parseXml(XML.load(inFoods))

    println ("Foods/categories with missing descriptions:")

    findMissingDescriptions(rootCat).foreach ( x => println (x.fullCode) )
    
    println ("Foods with no portion size definition:")
    
    findMissingPortionSize(rootCat).sortBy(_.description).foreach ( x => println ( x.fullCode + " " + x.description))
  }
}