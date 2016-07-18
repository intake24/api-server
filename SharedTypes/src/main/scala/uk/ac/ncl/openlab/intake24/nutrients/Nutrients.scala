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

package uk.ac.ncl.openlab.intake24.nutrients

import scala.collection.JavaConversions._

sealed trait Nutrient {
  val key: String
  val id: Int
}

case object Protein extends Nutrient { val key = "protein"; val id = 1 } 
case object Fat extends Nutrient { val key = "fat"; val id = 2 }
case object Carbohydrate extends Nutrient { val key = "carbohydrate"; val id = 3 }
case object EnergyKcal extends Nutrient { val key = "energy_kcal"; val id = 4 }
case object EnergyKj extends Nutrient { val key = "energy_kj"; val id = 5 }
case object Alcohol extends Nutrient { val key = "alcohol"; val id = 6 }
case object TotalSugars extends Nutrient { val key = "total_sugars"; val id = 7 }
case object Nmes extends Nutrient { val key = "nmes"; val id = 8 }
case object SatdFa extends Nutrient { val key = "satd_fa"; val id = 9 }
case object Cholesterol extends Nutrient { val key = "cholesterol"; val id = 10 }
case object VitaminA extends Nutrient { val key = "vitamin_a"; val id = 11 }
case object VitaminD extends Nutrient { val key = "vitamin_d"; val id = 12 }
case object VitaminC extends Nutrient { val key = "vitamin_c"; val id = 13 }
case object VitaminE extends Nutrient { val key = "vitamin_e"; val id = 14 }
case object Folate extends Nutrient { val key = "folate"; val id = 15 }
case object Sodium extends Nutrient { val key = "sodium"; val id = 16 }
case object Calcium extends Nutrient { val key = "calcium"; val id = 17 }
case object Iron extends Nutrient { val key = "iron"; val id = 18 }
case object Zinc extends Nutrient { val key = "zinc"; val id = 19 }
case object Selenium extends Nutrient { val key = "selenium"; val id = 20 }

object Nutrient {
  val list: Seq[Nutrient] = Seq(Protein, Fat, Carbohydrate, EnergyKcal, EnergyKj, Alcohol, TotalSugars, Nmes, SatdFa, Cholesterol,
    VitaminA, VitaminD, VitaminC, VitaminE, Folate, Sodium, Calcium, Iron, Zinc, Selenium)
    
  val javaList: java.util.List[Nutrient] = list
}

case class NutrientData(description: Map[String, String], value: Map[String, Map[String, java.lang.Double]])

