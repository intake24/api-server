package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import uk.ac.ncl.openlab.intake24.nutrientsndns.{CsvNutrientTableMapping, CsvNutrientTableParser, LegacyNutrientTables}

object IndiaCsvMapping {

  import CsvNutrientTableParser.{excelColumnToOffset => col}

  val mapping = Map(
    1l -> col("D"),
    11l -> col("E"),
    13l -> col("G"),
    17l -> col("H"),
    49l -> col("F"),
    114l -> col("L"),
    115l -> col("K"),
    123l -> col("M"),
    124l -> col("N"),
    125l -> col("O"),
    129l -> col("P"),
    138l -> col("Q"),
    139l -> col("R"),
    140l -> col("I"),
    143l -> col("J"))
}

object IndiaImport extends CsvNutrientTableImport("INDIA", "Indian food composition table", CsvNutrientTableMapping(1, 0, 2, Some(2), IndiaCsvMapping.mapping))
