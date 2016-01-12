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

package net.scran24.fooddef.nutrients

import java.io.FileReader
import scala.collection.JavaConversions._

sealed trait Nutrient {
  val key: String
}

case object Protein extends Nutrient { val key = "protein" } 
case object Fat extends Nutrient { val key = "fat" }
case object Carbohydrate extends Nutrient { val key = "carbohydrate" }
case object EnergyKcal extends Nutrient { val key = "energy_kcal" }
case object EnergyKj extends Nutrient { val key = "energy_kj" }
case object Alcohol extends Nutrient { val key = "alcohol" }
case object TotalSugars extends Nutrient { val key = "total_sugars" }
case object Nmes extends Nutrient { val key = "nmes" }
case object SatdFa extends Nutrient { val key = "satd_fa" }
case object Cholesterol extends Nutrient { val key = "cholesterol" }
case object VitaminA extends Nutrient { val key = "vitamin_a" }
case object VitaminD extends Nutrient { val key = "vitamin_d" }
case object VitaminC extends Nutrient { val key = "vitamin_c" }
case object VitaminE extends Nutrient { val key = "vitamin_e" }
case object Folate extends Nutrient { val key = "folate" }
case object Sodium extends Nutrient { val key = "sodium" }
case object Calcium extends Nutrient { val key = "calcium" }
case object Iron extends Nutrient { val key = "iron" }
case object Zinc extends Nutrient { val key = "zinc" }
case object Selenium extends Nutrient { val key = "selenium" }

object Nutrient {
  val list: Seq[Nutrient] = Seq(Protein, Fat, Carbohydrate, EnergyKcal, EnergyKj, Alcohol, TotalSugars, Nmes, SatdFa, Cholesterol,
    VitaminA, VitaminD, VitaminC, VitaminE, Folate, Sodium, Calcium, Iron, Zinc, Selenium)
    
  val javaList: java.util.List[Nutrient] = list
}

case class NutrientData(description: Map[String, String], value: Map[Int, Map[String, java.lang.Double]])

