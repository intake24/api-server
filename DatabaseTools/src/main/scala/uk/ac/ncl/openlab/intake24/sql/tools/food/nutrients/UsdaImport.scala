package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import uk.ac.ncl.openlab.intake24.nutrientsndns.{CsvNutrientTableMapping, LegacyNutrientTables}

object UsdaImport extends CsvNutrientTableImport("USDA", "USDA Food Composition Database", CsvNutrientTableMapping(1, 0, 1, None, ArabicCompositionTableMappings.USDA))