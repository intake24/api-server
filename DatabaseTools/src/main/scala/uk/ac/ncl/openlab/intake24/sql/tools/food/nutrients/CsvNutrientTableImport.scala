package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.admin.NutrientTablesAdminImpl
import uk.ac.ncl.openlab.intake24.nutrientsndns.{CsvNutrientTableMapping, CsvNutrientTableParser}
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, WarningMessage}
import uk.ac.ncl.openlab.intake24.{NewNutrientTableRecord, NutrientTable}

abstract class CsvNutrientTableImport(nutrientTableId: String, nutrientTableDescription: String, nutrientMapping: CsvNutrientTableMapping) extends App with WarningMessage with DatabaseConnection {

  trait Options extends DatabaseConfigurationOptions {

    val csvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  displayWarningMessage(s"This will destroy all food records having $nutrientTableId code in ${dbConfig.host}/${dbConfig.database}. Are you sure?")

  val dataSource = getDataSource(dbConfig)

  val nutrientTableService = new NutrientTablesAdminImpl(dataSource)

  nutrientTableService.deleteNutrientTable(nutrientTableId)

  nutrientTableService.createNutrientTable(NutrientTable(nutrientTableId, nutrientTableDescription))

  val records = CsvNutrientTableParser.parseTable(options.csvPath(), nutrientMapping)

  val newRecords = records.map {
    record =>
      NewNutrientTableRecord(record.id, nutrientTableId, record.description, None, record.nutrients)
  }.toSeq

  nutrientTableService.createNutrientTableRecords(newRecords)
}
