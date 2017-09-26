package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableMapping
import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableParser.{excelColumnToOffset => col}

object PortugueseNutrientMapping extends CsvNutrientTableMapping(
  3, // first row offset
  0, // record id column offset
  3,
  Some(2),
  Map(
    1l -> col("E"),
    2l -> col("F"),
    8l -> col("G"),
    11l -> col("H"),
    13l -> col("J"),
    17l -> col("Q"),
    20l -> col("N"),
    21l -> col("O"),
    32l -> col("K"),
    33l -> col("L"),
    47l -> col("M"),
    48l -> col("P"),
    49l -> col("I"),
    50l -> col("R"),
    51l -> col("S"),
    52l -> col("T"),
    58l -> col("U"),
    59l -> col("W"),
    115l -> col("Z"),
    121l -> col("Y"),
    122l -> col("AA"),
    123l -> col("AC"),
    124l -> col("AD"),
    125l -> col("AE"),
    126l -> col("AG"),
    128l -> col("AF"),
    129l -> col("AJ"),
    132l -> col("AH"),
    133l -> col("AI"),
    134l -> col("AK"),
    138l -> col("AM"),
    139l -> col("AN"),
    140l -> col("AO"),
    141l -> col("AQ"),
    142l -> col("AP"),
    143l -> col("AR"),
    147l -> col("AS"),
    153l -> col("V"),
    154l -> col("X"),
    155l -> col("AB"),
    157l -> col("AL")
  ))

