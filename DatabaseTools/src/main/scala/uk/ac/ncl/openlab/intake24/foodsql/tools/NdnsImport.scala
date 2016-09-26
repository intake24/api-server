package uk.ac.ncl.openlab.intake24.foodsql.tools

import org.slf4j.LoggerFactory
import org.rogach.scallop.ScallopConf

import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.nutrients.Nutrient
import uk.ac.ncl.openlab.intake24.NutrientTableRecord
import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableParser
import uk.ac.ncl.openlab.intake24.nutrientsndns.LegacyNutrientTables
import uk.ac.ncl.openlab.intake24.foodsql.admin.NutrientTablesAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl

object NdnsImport extends App with WarningMessage with DatabaseConnection {

  val ndnsTableCode = "NDNS"
  val ndnsTableDescription = "UK National Diet and Nutrition Survey"

  val logger = LoggerFactory.getLogger(getClass)

  trait Options extends ScallopConf {
    version("Intake24 NDNS nutrient table import tool 16.7")

    val csvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  displayWarningMessage("WARNING: THIS WILL DESTROY ALL FOOD RECORDS HAVING NDNS CODES!")

  val dataSource = getDataSource(options)

  val nutrientTableService = new FoodDatabaseAdminImpl (dataSource)

  nutrientTableService.deleteNutrientTable(ndnsTableCode)

  nutrientTableService.createNutrientTable(NutrientTable(ndnsTableCode, ndnsTableDescription))

  val table = CsvNutrientTableParser.parseTable(options.csvPath(), LegacyNutrientTables.ndnsCsvTableMapping)

  val records = table.records.map {
    case (code, nmap) =>
      NutrientTableRecord(ndnsTableCode, code, nmap)
  }.toSeq
 
  nutrientTableService.createNutrientTableRecords(records)

}
