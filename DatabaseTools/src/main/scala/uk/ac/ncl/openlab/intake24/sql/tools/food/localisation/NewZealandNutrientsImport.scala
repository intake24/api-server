package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import uk.ac.ncl.openlab.intake24.nutrientsndns.LegacyNutrientTables
import uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients.CsvNutrientTableImport

object NewZealandNutrientsImport extends CsvNutrientTableImport("NZ", "New Zealand Nutrient Database", LegacyNutrientTables.nzCsvTableMapping)
