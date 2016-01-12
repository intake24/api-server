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
import uk.ac.ncl.openlab.intake24.foodxml.FoodDefOld
import uk.ac.ncl.openlab.intake24.foodxml.Util


object ConvertToPortionSizeScripts {
  def main(args: Array[String]): Unit = {
    val foods = FoodDefOld.parseXml(XML.load("D:\\SCRAN24\\Data\\foods.xml"))

    FoodDefOld.writeXml("D:\\scratch\\foods-simple-portionsize.xml", Util.mapFoods(foods, f => f.portionSize match {
      // case asServed: AsServedRef => f.copy (portionSize = ScriptDef("as-served", Seq(("serving-image-set", asServed.setId), ("leftovers-image-set", asServed.setId + "_leftovers"))))
      //case guide: GuideDef => f.copy (portionSize = ScriptDef("guide-image", Seq(("guide-image-id", guide.portionSizeGuide))))
      //case drinkscale: DrinkwareRef => f.copy (portionSize = ScriptDef("drink-scale", Seq(("drinkware-id", drinkscale.id))))
      //case script: ScriptDef if script.name == "drink-scale" => f.copy (portionSize = ScriptDef( script.name, script.parameters ++ Seq(("initial-fill-level", "0.85"))))
      case _ => f
    }))
  }
}