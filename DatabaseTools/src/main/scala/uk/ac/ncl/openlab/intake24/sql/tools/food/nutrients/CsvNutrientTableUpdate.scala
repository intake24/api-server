package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.NutrientTableRecord
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.nutrientsndns.{CsvNutrientTableMapping, CsvNutrientTableParser}
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, WarningMessage}

abstract class CsvNutrientTableUpdate(nutrientTableId: String, nutrientMapping: CsvNutrientTableMapping) extends App with WarningMessage with DatabaseConnection {

  trait Options extends DatabaseConfigurationOptions {

    val csvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  displayWarningMessage(s"This will update $nutrientTableId nutrient records in ${dbConfig.host}/${dbConfig.database}. Are you sure?")

  val dataSource = getDataSource(dbConfig)

  val nutrientTableService = new FoodDatabaseAdminImpl(dataSource)

  val table = CsvNutrientTableParser.parseTable(options.csvPath(), nutrientMapping)

  val records = table.records.map {
    case (code, nmap) =>
      NutrientTableRecord(nutrientTableId, code, nmap)
  }.toSeq
 
  nutrientTableService.updateNutrientTableRecords(records)

}
