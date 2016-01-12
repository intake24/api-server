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
import uk.ac.ncl.openlab.intake24.foodxml.CategoryDef
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.foodxml.Util

object DryRun extends App {
  val foods = FoodDef.parseXml(XML.load("D:\\SCRAN24\\Data\\foods.xml"))
  Util.writeXml(FoodDef.toXml(foods), "D:\\SCRAN24\\Data\\foods-dryrun.xml")
  
  val cats = CategoryDef.parseXml(XML.load("D:\\SCRAN24\\Data\\categories.xml"))
  Util.writeXml(CategoryDef.toXml(cats), "D:\\SCRAN24\\Data\\categories-dryrun.xml")

}