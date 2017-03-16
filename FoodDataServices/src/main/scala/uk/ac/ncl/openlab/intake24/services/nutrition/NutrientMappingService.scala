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

import uk.ac.ncl.openlab.intake24.errors.{NutrientMappingError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.surveydata.{NutrientMappedSubmission, SurveySubmission}

case class NutrientDescription(nutrientId: Long, description: String, unit: String)

trait NutrientMappingService {
  def supportedNutrients(): Either[UnexpectedDatabaseError, Seq[NutrientDescription]]
  
  def energyKcalNutrientId(): Long
  
  def nutrientsFor(table_id: String, record_id: String, weight: Double): Either[NutrientMappingError, Map[Long, Double]]

  def javaNutrientsFor(table_id: String, record_id: String, weight: Double): Either[NutrientMappingError, java.util.Map[java.lang.Long, java.lang.Double]] =
    nutrientsFor(table_id, record_id, weight).right.map(scalaMap => scala.collection.JavaConversions.mapAsJavaMap(scalaMap.map {
      case (k, v) => (new java.lang.Long(k), new java.lang.Double(v))
    }))

  def mapSubmission(submission: SurveySubmission, locale: String): Either[NutrientMappingError, NutrientMappedSubmission]
}
