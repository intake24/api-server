package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import uk.ac.ncl.openlab.intake24.nutrientsndns.LegacyNutrientTables

object NdnsImport extends CsvNutrientTableImport("NDNS", "UK National Diet and Nutrition Survey", LegacyNutrientTables.ndnsCsvTableMapping)