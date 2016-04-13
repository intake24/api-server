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

package uk.ac.ncl.openlab.intake24.nutrientsndns

import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory
import com.google.inject.Singleton
import com.google.inject.name.Named
import com.google.inject.Inject
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientDescription
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientMappingService
import uk.ac.ncl.openlab.intake24.services.FoodDataService
import net.scran24.fooddef.nutrients.Fat
import net.scran24.fooddef.nutrients.Nmes
import net.scran24.fooddef.nutrients.Protein
import net.scran24.fooddef.nutrients.Selenium
import net.scran24.fooddef.nutrients.VitaminE
import net.scran24.fooddef.nutrients.Folate
import net.scran24.fooddef.nutrients.SatdFa
import net.scran24.fooddef.nutrients.Alcohol
import net.scran24.fooddef.nutrients.Cholesterol
import net.scran24.fooddef.nutrients.VitaminA
import net.scran24.fooddef.nutrients.Zinc
import net.scran24.fooddef.nutrients.Iron
import net.scran24.fooddef.nutrients.Sodium
import net.scran24.fooddef.nutrients.Calcium
import net.scran24.fooddef.nutrients.EnergyKj
import net.scran24.fooddef.nutrients.VitaminD
import net.scran24.fooddef.nutrients.Carbohydrate
import net.scran24.fooddef.nutrients.TotalSugars
import net.scran24.fooddef.nutrients.VitaminC
import net.scran24.fooddef.nutrients.EnergyKcal
import net.scran24.fooddef.nutrients.Nutrient

@Singleton
class NzNutrientMappingServiceImpl @Inject() (@Named("nz-data-path") nutrientTableFile: String, foodData: FoodDataService) extends NutrientMappingService {
  val tableMapping: Map[Nutrient, Int] =
    Map(
      (Protein, 20),
      (Fat, 22),
      (Carbohydrate, 24),
      (EnergyKcal, 26),
      (EnergyKj, 28),
      (Alcohol, 30),
      (TotalSugars, 38),
      (Nmes, 40),
      (SatdFa, 56),
      (Cholesterol, 66),
      (VitaminA, 78),
      (VitaminD, 80),
      (VitaminC, 92),
      (VitaminE, 94),
      (Folate, 100),
      (Sodium, 106),
      (Calcium, 110),
      (Iron, 116),
      (Zinc, 124),
      (Selenium, 132))

  val table = CsvNutrientTableParser.parseTable(nutrientTableFile, 2, tableMapping)
  val log = LoggerFactory.getLogger(classOf[NzNutrientMappingServiceImpl]);

  def nutrientDescriptions() =
    Seq(NutrientDescription("protein", "Protein", "g"),
      NutrientDescription("fat", "Fat", "g"),
      NutrientDescription("carbohydrate", "Carbohydrate", "g"),
      NutrientDescription("energy_kcal", "Energy", "kcal"),
      NutrientDescription("energy_kj", "Energy", "kJ"),
      NutrientDescription("alcohol", "Alcohol", "g"),
      NutrientDescription("total_sugars", "Total sugars", "g"),
      NutrientDescription("nmes", "Non milk extrinsic sugars", "g"),
      NutrientDescription("satd_fa", "Saturated fat", "g"),
      NutrientDescription("cholesterol", "Cholesterol", "mg"),
      NutrientDescription("vitamin_a", "Vitamin A", "µg"),
      NutrientDescription("vitamin_d", "Vitamin D", "µg"),
      NutrientDescription("vitamin_c", "Vitamin C", "mg"),
      NutrientDescription("vitamin_e", "Vitamin E", "mg"),
      NutrientDescription("folate", "Folate", "g"),
      NutrientDescription("sodium", "Sodium", "mg"),
      NutrientDescription("calcium", "Calcium", "mg"),
      NutrientDescription("iron", "Iron", "mg"),
      NutrientDescription("zinc", "Zinc", "mg"),
      NutrientDescription("selenium", "Selenium", "µg"))

  def nutrientsFor(foodCode: String, weight: Double): Map[String, Double] = {
    table.value.get(foodCode) match {
      case Some(data) => data.mapValues(v => double2Double(v / 100.0 * weight))
      case None => CsvNutrientTableParser.zeroData
    }
  }
}