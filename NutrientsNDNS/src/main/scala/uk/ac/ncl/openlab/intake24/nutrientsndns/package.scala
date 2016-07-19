package uk.ac.ncl.openlab.intake24

import uk.ac.ncl.openlab.intake24.nutrients.Nutrient

package object nutrientsndns {
  type NutrientTable = Map[String, Map[Nutrient, Double]]
}
