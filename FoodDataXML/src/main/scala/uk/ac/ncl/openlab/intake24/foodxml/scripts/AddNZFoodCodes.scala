package uk.ac.ncl.openlab.intake24.foodxml.scripts

import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import scala.xml.XML
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import collection.JavaConversions._

object AddNZFoodCodes extends App {

  val foods = FoodDef.parseXml(XML.load("/home/ivan/Projects/Intake24/intake24-data-NEW ZEALAND/foods.xml"))
  
  val reader1 = new CSVReader(new FileReader("/home/ivan/Projects/Intake24/NZ food data/Intake24 to NZ codes.csv"))
  val reader2 = new CSVReader(new FileReader("/home/ivan/Projects/Intake24/NZ food data/Intake24 to NZ codes 2.csv"))
  
  val lines1 = reader1.readAll()  
  reader1.close()
  
  val lines2 = reader2.readAll()
  reader2.close()
  
  val map = lines1.toSeq.tail.map(row => (row(0), row(1))).toMap ++ lines2.toSeq.tail.map(row => (row(0), row(1))).toMap 
  
  val updated = foods.map {
    food =>
      if (map.contains(food.code)) {
        println ("Using " + map(food.code) + " as NZ code for " + food.code)
        food.copy(localData = food.localData.copy(nutrientTableCodes = food.localData.nutrientTableCodes + ("NZ" -> map(food.code))))
      }
      else
        food
  }
  
  updated.foreach {
    food =>
      if (food.localData.nutrientTableCodes.isEmpty)
        println("No nutrient table code: " + food.code)
      if (food.localData.nutrientTableCodes.get("NZ").nonEmpty && food.localData.nutrientTableCodes.get("NDNS").nonEmpty)
        println("Both tables: " + food.code)
  }
  
  XML.save("/home/ivan/Projects/Intake24/intake24-data-NEW ZEALAND/foods-nzcodes.xml", FoodDef.toXml(updated))
}