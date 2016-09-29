package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory
import com.google.inject.Inject
import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.NutrientTableRecord

import uk.ac.ncl.openlab.intake24.nutrientsndns.CsvNutrientTableParser
import uk.ac.ncl.openlab.intake24.nutrientsndns.LegacyNutrientTables
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseConnection
import uk.ac.ncl.openlab.intake24.sql.tools.DatabaseOptions
import uk.ac.ncl.openlab.intake24.sql.tools.WarningMessage
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl

object NewZealandNutrientsImport extends App with WarningMessage with DatabaseConnection {

  val nzTableCode = "NZ"
  val nzTableDescription = "New Zealand Nutrient Database"

  val logger = LoggerFactory.getLogger(getClass)

  trait Options extends ScallopConf {
    version("Intake24 New Zealand nutrient table import tool 16.7")

    val csvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options with DatabaseOptions

  options.afterInit()

  displayWarningMessage("WARNING: THIS WILL DESTROY ALL FOOD RECORDS HAVING NZ CODES!")

  val dataSource = getDataSource(options)

  val nutrientTableService = new FoodDatabaseAdminImpl(dataSource)

  nutrientTableService.deleteNutrientTable(nzTableCode)

  nutrientTableService.createNutrientTable(NutrientTable(nzTableCode, nzTableDescription))

  val table = CsvNutrientTableParser.parseTable(options.csvPath(), LegacyNutrientTables.nzCsvTableMapping)

  val records = table.records.map {
    case (code, nmap) =>
      NutrientTableRecord(nzTableCode, code, nmap)
  }.toSeq

  nutrientTableService.createNutrientTableRecords(records)

}
