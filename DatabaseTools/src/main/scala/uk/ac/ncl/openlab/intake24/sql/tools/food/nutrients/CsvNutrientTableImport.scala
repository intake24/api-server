package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.nutrientsndns.{CsvNutrientTableMapping, CsvNutrientTableParser}
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, WarningMessage}
import uk.ac.ncl.openlab.intake24.{NutrientTable, NutrientTableRecord}

abstract class CsvNutrientTableImport(nutrientTableId: String, nutrientTableDescription: String, nutrientMapping: CsvNutrientTableMapping) extends App with WarningMessage with DatabaseConnection {

  trait Options extends DatabaseConfigurationOptions {

    val csvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  displayWarningMessage(s"This will destroy all food records having $nutrientTableId code in ${dbConfig.host}/${dbConfig.database}. Are you sure?")

  val dataSource = getDataSource(dbConfig)

  val nutrientTableService = new FoodDatabaseAdminImpl(dataSource)

  nutrientTableService.deleteNutrientTable(nutrientTableId)

  nutrientTableService.createNutrientTable(NutrientTable(nutrientTableId, nutrientTableDescription))

  val table = CsvNutrientTableParser.parseTable(options.csvPath(), nutrientMapping)

  val records = table.records.map {
    case (code, nmap) =>
      NutrientTableRecord(nutrientTableId, code, nmap)
  }.toSeq

  nutrientTableService.createNutrientTableRecords(records)
}
