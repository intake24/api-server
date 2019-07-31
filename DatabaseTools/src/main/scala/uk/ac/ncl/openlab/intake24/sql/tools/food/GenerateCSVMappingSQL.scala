package uk.ac.ncl.openlab.intake24.sql.tools.food

import uk.ac.ncl.openlab.intake24.nutrientsndns.{CsvNutrientTableMapping, CsvNutrientTableParser, LegacyNutrientTables}
import uk.ac.ncl.openlab.intake24.sql.tools.food.localisation.{DanishNutrientMapping, PortugueseNutrientMapping}
import uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients.{ArabicCompositionTableMappings, IndiaCsvMapping, UkgfImport}

object GenerateCSVMappingSQL extends App {
  private def generateSQL(tableId: String, mapping: CsvNutrientTableMapping): String = {

    val sb = new StringBuilder()

    sb.append("insert into nutrient_table_csv_mapping(nutrient_table_id, row_offset, id_column_offset, description_column_offset, local_description_column_offset) values\n")
    sb.append(s"('$tableId', ${mapping.rowOffset}, ${mapping.idColumn}, ${mapping.descriptionColumn}, ${mapping.localDescriptionColumn.getOrElse("null")});\n\n")

    sb.append("insert into nutrient_table_csv_mapping_columns(nutrient_table_id, nutrient_type_id, column_offset) values\n")

    val nutrientValues = mapping.nutrientMapping.toSeq.map {
      case (nutrientId, columnOffset) =>
        s"('$tableId', $nutrientId, ${columnOffset - 1})"
    }

    sb.append(nutrientValues.mkString(",\n"))
    sb.append(";\n")

    sb.toString()
  }


  println(generateSQL("NDNS", LegacyNutrientTables.ndnsCsvTableMapping))
  println(generateSQL("NZ", LegacyNutrientTables.nzCsvTableMapping))
  println(generateSQL("PT_INSA", PortugueseNutrientMapping))
  println(generateSQL("DK_DTU", DanishNutrientMapping))
  println(generateSQL("UK_GF", LegacyNutrientTables.ndnsCsvTableMapping))
  println(generateSQL("USDA", CsvNutrientTableMapping(1, 0, 1, None, ArabicCompositionTableMappings.USDA)))
  println(generateSQL("INDIA", CsvNutrientTableMapping(1, 0, 2, Some(2), IndiaCsvMapping.mapping)))

}
