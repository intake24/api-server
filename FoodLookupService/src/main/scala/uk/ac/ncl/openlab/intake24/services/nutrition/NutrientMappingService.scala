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

package uk.ac.ncl.openlab.intake24.services.nutrition

import uk.ac.ncl.openlab.intake24.nutrients.Nutrient

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.NutrientMappingError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

case class NutrientDescription(nutrientType: Nutrient, description: String, unit: String)

trait NutrientMappingService {
  def supportedNutrients(): Either[DatabaseError, Seq[NutrientDescription]]
  def nutrientsFor(table_id: String, record_id: String, weight: Double): Either[NutrientMappingError, Map[Nutrient, Double]]

  def javaNutrientsFor(table_id: String, record_id: String, weight: Double): Either[NutrientMappingError, java.util.Map[Nutrient, java.lang.Double]] =
    nutrientsFor(table_id, record_id, weight).right.map(scalaMap => scala.collection.JavaConversions.mapAsJavaMap(scalaMap.mapValues(new java.lang.Double(_))))
}
