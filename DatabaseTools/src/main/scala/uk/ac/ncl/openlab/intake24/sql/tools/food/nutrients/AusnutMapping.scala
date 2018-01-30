package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableParser

object AusnutMapping {

  import CsvNutrientTableParser.{excelColumnToOffset => col}

  val map = Map(1l -> col("D"))

}
