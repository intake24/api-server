package uk.ac.ncl.openlab.intake24.foodsql.tools

import org.slf4j.LoggerFactory
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.NutrientDataManagementSqlImpl
import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.nutrientsndns.NdnsNutrientMappingServiceImpl
import uk.ac.ncl.openlab.intake24.nutrients.Nutrient
import uk.ac.ncl.openlab.intake24.NutrientTableRecord

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
  
  val nutrientTableService = new NutrientDataManagementSqlImpl(dataSource)
  
  nutrientTableService.deleteNutrientTable(ndnsTableCode)
  
  nutrientTableService.createNutrientTable(NutrientTable(ndnsTableCode, ndnsTableDescription))
  
  val ndnsMapper = new NdnsNutrientMappingServiceImpl(options.csvPath())
  
  val records = ndnsMapper.table.value.keySet.toSeq.flatMap {
    code =>
      Nutrient.list.map {
        nutrientType =>
          NutrientTableRecord(ndnsTableCode, code, nutrientType.id, ndnsMapper.table.value(code)(nutrientType.key))
      }
  }
  
  nutrientTableService.createNutrientTableRecords(records)

}
