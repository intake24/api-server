package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation.newzealand

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigChooser, DatabaseConnection, WarningMessage}
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodsAdminImpl
import uk.ac.ncl.openlab.intake24.sql.tools.food.ImportNutrientTableDescriptions.{getDataSource, options}

import scala.xml.XML

object ImportNewZealandFoods extends App with DatabaseConnection with WarningMessage {

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)
    val xmlDataDir = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())
  val dataSource = getDataSource(databaseConfig)

  println ("Parsing XML food data...")

  val foodsFromXml = FoodDef.parseXml(XML.load(options.xmlDataDir() + "/foods.xml"))

  val foodsAdminService = new FoodsAdminImpl(dataSource)

  foodsFromXml.foreach {
    food =>
      print (food.code + "... ")
      if (foodsAdminService.getFoodRecord(food.code, "en_GB").isRight)
        println(" exists in en_GB")
      else
        println(" does not exist in en_GB")
  }

}
