package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients.CsvNutrientTableImport

object DanishNutrientsImport extends CsvNutrientTableImport("DK_DTU", "Danish Food Composition Table (DTU)", DanishNutrientMapping)
