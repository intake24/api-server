package uk.ac.ncl.openlab.intake24.sql.tools.food

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools.food.MigrateFoodDatabase.args
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConnection, WarningMessage}

/**
  * Created by Tim Osadchiy on 22/05/2017.
  */
object ImportNutrientTableDescriptions extends App with DatabaseConnection with WarningMessage {

  val UK_CSV_NAME = "NDNS"
  val DK_CSV_NAME = "DK_DTU"
  val NZ_CSV_NAME = "NZ"
  val PT_CSV_NAME = "PT_INSA"

  private def apply() = {
    val options = new ScallopConf(args) {
      val dbConfigDir = opt[String](required = true)
      val csvDir = opt[String](required = true)
    }

    options.verify()

    println(options.csvDir())
  }

  apply()

}
