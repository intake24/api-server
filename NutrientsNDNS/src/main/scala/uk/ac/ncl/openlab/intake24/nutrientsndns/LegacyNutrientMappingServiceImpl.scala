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
import uk.ac.ncl.openlab.intake24.nutrients._
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.NutrientMappingError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.TableNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordType

@Singleton
case class LegacyNutrientMappingServiceImpl @Inject() (tables: Map[String, NutrientTable]) extends NutrientMappingService {
  
  private def legacyDescription(nutrientType: Nutrient) = nutrientType match {
    case Protein => NutrientDescription(Protein, "Protein", "g")
    case Fat => NutrientDescription(Fat, "Fat", "g")
    case Carbohydrate => NutrientDescription(Carbohydrate, "Carbohydrate", "g")
    case EnergyKcal => NutrientDescription(EnergyKcal, "Energy", "kcal")
    case EnergyKj => NutrientDescription(EnergyKj, "Energy", "kJ")
    case Alcohol => NutrientDescription(Alcohol, "Alcohol", "g")
    case TotalSugars => NutrientDescription(TotalSugars, "Total sugars", "g")
    case Nmes => NutrientDescription(Nmes, "Non milk extrinsic sugars", "g")
    case SaturatedFattyAcids => NutrientDescription(SaturatedFattyAcids, "Saturated fat", "g")
    case Cholesterol => NutrientDescription(Cholesterol, "Cholesterol", "mg")
    case VitaminA => NutrientDescription(VitaminA, "Vitamin A", "µg")
    case VitaminD => NutrientDescription(VitaminD, "Vitamin D", "µg")
    case VitaminC => NutrientDescription(VitaminC, "Vitamin C", "mg")
    case VitaminE => NutrientDescription(VitaminE, "Vitamin E", "mg")
    case Folate => NutrientDescription(Folate, "Folate", "g")
    case Sodium => NutrientDescription(Sodium, "Sodium", "mg")
    case Calcium => NutrientDescription(Calcium, "Calcium", "mg")
    case Iron => NutrientDescription(Iron, "Iron", "mg")
    case Zinc => NutrientDescription(Zinc, "Zinc", "mg")
    case Selenium => NutrientDescription(Selenium, "Selenium", "µg")
    case DietaryFiber => NutrientDescription(Selenium, "Dietary fiber", "g")
    case TotalMonosaccharides => NutrientDescription(Selenium, "Total monosaccharides", "g")
    case OrganicAcids => NutrientDescription(Selenium, "Organic acids", "g")
    case PolyunsaturatedFattyAcids => NutrientDescription(Selenium, "Polyunsaturated fatty acids", "g")
    case NaCl => NutrientDescription(Selenium, "NaCl", "mg")
    case Ash => NutrientDescription(Selenium, "Ash", "g")
  }

  def supportedNutrients() = Right(Nutrient.types.toSeq.sortBy(_.id).map(legacyDescription))

  val log = LoggerFactory.getLogger(classOf[LegacyNutrientMappingServiceImpl])

  def nutrientsFor(table_id: String, record_id: String, weight: Double): Either[NutrientMappingError, Map[Nutrient, Double]] = tables.get(table_id) match {
    case Some(table) => table.records.get(record_id) match {
      case Some(record) => Right(record.mapValues(v => v / 100.0 * weight))
      case None => Left(RecordNotFound(RecordType.NutrientTableRecord, s"$table_id/$record_id"))
    }
    case None => Left(TableNotFound)
  }
}