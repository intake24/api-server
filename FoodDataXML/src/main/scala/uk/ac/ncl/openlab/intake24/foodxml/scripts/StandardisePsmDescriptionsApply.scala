package uk.ac.ncl.openlab.intake24.foodxml.scripts

import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import scala.xml.XML
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import au.com.bytecode.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import uk.ac.ncl.openlab.intake24.foodxml.Util

object StandardisePsmDescriptionsApply extends App {

  val dataDir = "/home/ivan/Projects/Intake24/intake24-data"

  val foods = FoodDef.parseXml(XML.load(dataDir + "/foods.xml"))
  
  val reader = new CSVReader(new FileReader("/home/ivan/tmp/psm.csv"))
  
  val rows = reader.readAll().tail.filter(row => !row(2).isEmpty())
  
  val descMap = rows.map(row => (row(2).trim().toLowerCase() -> row(0).trim().toLowerCase().replace(" ", "_"))).toMap  ++ Map("bowlfuls" -> "in_a_bowl", "choose a slice" -> "in slices")
  
  
 
  val processed = foods.map {
    food =>
      food.copy(portionSizeMethods = food.portionSizeMethods.map(ps => ps.copy( description =  descMap(ps.description.trim().toLowerCase())))) 
  }
  
   Util.writeXml(FoodDef.toXml(processed), "/home/ivan/Projects/Intake24/intake24-data/foods.xml")

}
