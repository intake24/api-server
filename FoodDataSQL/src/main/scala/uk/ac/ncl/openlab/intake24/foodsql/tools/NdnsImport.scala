package uk.ac.ncl.openlab.intake24.foodsql.tools

import org.slf4j.LoggerFactory
import org.rogach.scallop.ScallopConf

object NdnsImport extends App with WarningMessage with DatabaseConnection {
  
  val ndnsTableCode = "NDNS"

  val logger = LoggerFactory.getLogger(getClass)

  case class Options(arguments: Seq[String]) extends ScallopConf(arguments) {
    version("Intake24 NDNS nutrient table import tool 16.7")

    val csvPath = opt[String](required = true, noshort = true)
  }

  val opts = Options(args)

  displayWarningMessage("WARNING: THIS WILL DESTROY ALL FOOD RECORDS HAVING NDNS CODES!")

  val dataSource = getDataSource(args)
  
  
  

}
