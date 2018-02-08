package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableMapping

object ArabicRecipesImport extends CsvNutrientTableImport("RECUAE", "UAE food recipes", CsvNutrientTableMapping(1, 0, 1, Some(3), ArabicCompositionTableMappings.Recipes))