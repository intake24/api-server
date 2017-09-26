package uk.ac.ncl.openlab.intake24.foodxml.scripts

import java.io.FileReader

import au.com.bytecode.opencsv.CSVReader
import uk.ac.ncl.openlab.intake24.foodxml.{FoodDef, Util}

import scala.collection.JavaConverters._
import scala.xml.XML

object StandardisePsmDescriptionsApply extends App {

  val dataDir = "/home/ivan/Projects/Intake24/intake24-data"

  val foods = FoodDef.parseXml(XML.load(dataDir + "/foods.xml"))

  val reader = new CSVReader(new FileReader("/home/ivan/tmp/psm.csv"))

  val rows = reader.readAll().asScala.tail.filter(row => !row(2).isEmpty())

  val descMap = rows.map(row => (row(2).trim().toLowerCase() -> row(0).trim().toLowerCase().replace(" ", "_"))).toMap ++ Map("bowlfuls" -> "in_a_bowl", "choose a slice" -> "in slices")


  val processed = foods.map {
    food =>
      food.copy(portionSizeMethods = food.portionSizeMethods.map(ps => ps.copy(description = descMap(ps.description.trim().toLowerCase()))))
  }

  Util.writeXml(FoodDef.toXml(processed), "/home/ivan/Projects/Intake24/intake24-data/foods.xml")

}
