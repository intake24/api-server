package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import uk.ac.ncl.openlab.intake24.nutrientsndns.LegacyNutrientTables
import uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients.CsvNutrientTableUpdate

object NewZealandNutrientsUpdate extends CsvNutrientTableUpdate("NZ", LegacyNutrientTables.nzCsvTableMapping)
