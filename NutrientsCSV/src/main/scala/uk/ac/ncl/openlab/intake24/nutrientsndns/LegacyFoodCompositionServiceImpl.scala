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

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{RecordNotFound, TableNotFound}
import uk.ac.ncl.openlab.intake24.services.nutrition.{FoodCompositionService, NutrientDescription}

@Singleton
case class LegacyFoodCompositionServiceImpl @Inject()(tables: Map[String, NutrientTable]) extends FoodCompositionService {

  def getSupportedNutrients() = Right(Seq(
    NutrientDescription(1, "Energy (kcal)", "kcal"),
    NutrientDescription(2, "Energy (kJ)", "kJ"),
    NutrientDescription(8, "Water", "g"),
    NutrientDescription(9, "Total nitrogen", "g"),
    NutrientDescription(10, "Nitrogen conversion factor", "g"),
    NutrientDescription(11, "Protein", "g"),
    NutrientDescription(13, "Carbohydrate", "g"),
    NutrientDescription(15, "Englyst fibre", "g"),
    NutrientDescription(16, "Southgate fibre", "g"),
    NutrientDescription(20, "Alcohol", "g"),
    NutrientDescription(21, "Starch", "g"),
    NutrientDescription(22, "Total sugars", "g"),
    NutrientDescription(23, "Non-milk extrinsic sugars", "g"),
    NutrientDescription(24, "Intrinsic and milk sugars", "g"),
    NutrientDescription(25, "Glucose", "g"),
    NutrientDescription(26, "Fructose", "g"),
    NutrientDescription(27, "Sucrose", "g"),
    NutrientDescription(28, "Maltose", "g"),
    NutrientDescription(29, "Lactose", "g"),
    NutrientDescription(30, "Other sugars (UK)", "g"),
    NutrientDescription(49, "Fat", "g"),
    NutrientDescription(50, "Satd FA", "g"),
    NutrientDescription(55, "Cis-Mon FA", "g"),
    NutrientDescription(56, "Cis-n3 FA", "g"),
    NutrientDescription(57, "Cis-n6 FA", "g"),
    NutrientDescription(58, "Trans FA", "g"),
    NutrientDescription(59, "Cholesterol", "g"),
    NutrientDescription(114, "Retinol", "g"),
    NutrientDescription(115, "Total carotene", "g"),
    NutrientDescription(116, "Alpha-carotene", "g"),
    NutrientDescription(117, "Beta-carotene", "g"),
    NutrientDescription(119, "Beta cryptoxanthin", "g"),
    NutrientDescription(120, "Vitamin A", "g"),
    NutrientDescription(122, "Vitamin D", "g"),
    NutrientDescription(123, "Thiamin", "g"),
    NutrientDescription(124, "Riboflavin", "g"),
    NutrientDescription(125, "Niacin", "g"),
    NutrientDescription(126, "Tryptophan/60", "g"),
    NutrientDescription(128, "Niacin equivalent", "g"),
    NutrientDescription(129, "Vitamin C", "g"),
    NutrientDescription(130, "Vitamin E", "g"),
    NutrientDescription(132, "Vitamin B6", "g"),
    NutrientDescription(133, "Vitamin B12", "g"),
    NutrientDescription(134, "Folate", "g"),
    NutrientDescription(136, "Pantothenic acid", "g"),
    NutrientDescription(137, "Biotin", "g"),
    NutrientDescription(138, "Sodium", "g"),
    NutrientDescription(139, "Potassium", "g"),
    NutrientDescription(140, "Calcium", "g"),
    NutrientDescription(141, "Magnesium", "g"),
    NutrientDescription(142, "Phosphorus", "g"),
    NutrientDescription(143, "Iron", "g"),
    NutrientDescription(144, "Haem iron", "g"),
    NutrientDescription(145, "Non-haem iron", "g"),
    NutrientDescription(146, "Copper", "g"),
    NutrientDescription(147, "Zinc", "g"),
    NutrientDescription(148, "Chloride", "g"),
    NutrientDescription(149, "Iodine", "g"),
    NutrientDescription(151, "Manganese", "g"),
    NutrientDescription(152, "Selenium", "g")))

  val log = LoggerFactory.getLogger(classOf[LegacyFoodCompositionServiceImpl])

  def getEnergyKcalNutrientId() = 1l

  def getFoodCompositionRecord(table_id: String, record_id: String) = tables.get(table_id) match {
    case Some(table) => table.records.get(record_id) match {
      case Some(record) => Right(record)
      case None => Left(RecordNotFound(new RuntimeException(s"table_id: $table_id, record_id: $record_id")))
    }
    case None => Left(TableNotFound(new RuntimeException(table_id)))
  }
}
