package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import uk.ac.ncl.openlab.intake24.nutrientsndns.{CsvNutrientTableMapping}

object AusnutImport extends CsvNutrientTableImport("AUSNUT", "AUSNUT 2011-13 food nutrient database", CsvNutrientTableMapping(1, 0, 2, None, AusnutMapping.map))