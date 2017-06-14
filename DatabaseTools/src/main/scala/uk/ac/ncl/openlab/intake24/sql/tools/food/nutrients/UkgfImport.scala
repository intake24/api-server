package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import uk.ac.ncl.openlab.intake24.nutrientsndns.LegacyNutrientTables

object UkgfImport extends CsvNutrientTableImport("UK_GF", "UK gluten-free foods", LegacyNutrientTables.ndnsCsvTableMapping)