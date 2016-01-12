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

package uk.ac.ncl.openlab.intake24.foodxml

import scala.Range
import net.scran24.fooddef.PortionSizeMethodParameter

case class StandardUnitDef(name: String, weight: Double, omitFoodDesc: Boolean)

object StandardUnitDef {
  def toPortionSizeParameters(units: Seq[StandardUnitDef]) = PortionSizeMethodParameter("units-count", units.size.toString) +: units.zipWithIndex.flatMap {
    case (unit, index) => {
      Seq(PortionSizeMethodParameter("unit" + index + "-name", unit.name),
        PortionSizeMethodParameter("unit" + index + "-weight", unit.weight.toString),
        PortionSizeMethodParameter("unit" + index + "-omit-food-description", if (unit.omitFoodDesc) "true" else "false"))
    }
  }

  def parsePortionSizeParameters(params: Seq[PortionSizeMethodParameter]) = {
    val map = params.map(p => (p.name, p.value)).toMap

    val number = map.get("units-count").map(_.toInt).getOrElse(0)

    Range(0, number).map(i => {
      val name = map("unit" + i + "-name")
      val weight = map("unit" + i + "-weight").toDouble
      val omit = map("unit" + i + "-omit-food-description") == "true"

      StandardUnitDef(name, weight, omit)
    })
  }
}