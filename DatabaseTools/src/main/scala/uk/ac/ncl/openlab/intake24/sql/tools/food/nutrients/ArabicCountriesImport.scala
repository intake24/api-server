package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import java.io.File

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.{NewNutrientTableRecord, NutrientTable}
import uk.ac.ncl.openlab.intake24.foodsql.admin.NutrientTablesAdminImpl
import uk.ac.ncl.openlab.intake24.nutrientsndns.{CsvNutrientTableMapping, CsvNutrientTableParser}
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, ErrorHandler, WarningMessage}

object ArabicCountriesImport extends App with WarningMessage with DatabaseConnection with ErrorHandler {

  import CsvNutrientTableParser.{excelColumnToOffset => col}

  val nutrientTableId = "GULF"

  val nutrientTableDescription = "Arab Gulf Countries"

  trait Options extends DatabaseConfigurationOptions {

    val csvDir = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  displayWarningMessage(s"This will update $nutrientTableId nutrient records in ${dbConfig.host}/${dbConfig.database}. Are you sure?")

  val dataSource = getDataSource(dbConfig)

  val nutrientTableService = new NutrientTablesAdminImpl(dataSource)


  val UAEMapping = CsvNutrientTableMapping(2, col("AG") - 1, 0, None, ArabicCompositionTableMappings.UAE)
  val BahrainMapping = CsvNutrientTableMapping(7, 0, 1, Some(2), ArabicCompositionTableMappings.Bahrain)
  val OmanMapping = CsvNutrientTableMapping(7, 0, 1, Some(2), ArabicCompositionTableMappings.Oman)
  val QatarMapping = CsvNutrientTableMapping(7, 0, 1, Some(2), ArabicCompositionTableMappings.Qatar)
  val SaudiMapping = CsvNutrientTableMapping(7, 0, 1, Some(2), ArabicCompositionTableMappings.Saudi)
  val KuwaitMapping = CsvNutrientTableMapping(2, 0, 1, Some(2),  ArabicCompositionTableMappings.Kuwait)

  val records =
    CsvNutrientTableParser.parseTable(options.csvDir() + File.separator + "uae.csv", UAEMapping) ++
      CsvNutrientTableParser.parseTable(options.csvDir() + File.separator + "bahrain.csv", BahrainMapping) ++
      CsvNutrientTableParser.parseTable(options.csvDir() + File.separator + "oman.csv", OmanMapping) ++
      CsvNutrientTableParser.parseTable(options.csvDir() + File.separator + "qatar.csv", QatarMapping) ++
      CsvNutrientTableParser.parseTable(options.csvDir() + File.separator + "saudi.csv", SaudiMapping) ++
      CsvNutrientTableParser.parseTable(options.csvDir() + File.separator + "kuwait.csv", KuwaitMapping)


  throwOnError(nutrientTableService.createOrUpdateNutrientTable(NutrientTable(nutrientTableId, nutrientTableDescription)))

  val newRecords = records.map {
    record =>
      NewNutrientTableRecord(record.id, nutrientTableId, record.description, record.localDescription, record.nutrients)
  }

  throwOnError(nutrientTableService.createOrUpdateNutrientTableRecords(newRecords))
}
