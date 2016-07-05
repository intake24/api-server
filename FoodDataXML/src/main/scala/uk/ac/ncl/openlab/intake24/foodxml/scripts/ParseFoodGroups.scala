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

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import uk.ac.ncl.openlab.intake24.FoodGroup
import uk.ac.ncl.openlab.intake24.foodxml.FoodGroupDef
import uk.ac.ncl.openlab.intake24.foodxml.Util

object ParseFoodGroups {
  def main(args: Array[String]) = {
    val sourcePath = "D:\\SCRAN24\\Notes\\food_groups.csv"

    // code | description																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									

    val groups = new CSVReader(new FileReader(sourcePath)).readAll().toList.map(_.toSeq).tail.distinct.map ( row => FoodGroup(row(0).toInt, row(1), Some(row(1))))
    
    Util.writeXml(FoodGroupDef.toXml(groups), "D:\\SCRAN24\\Data\\food-groups.xml")
  }
}