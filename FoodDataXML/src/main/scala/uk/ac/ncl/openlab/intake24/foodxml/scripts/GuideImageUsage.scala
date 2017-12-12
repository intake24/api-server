package uk.ac.ncl.openlab.intake24.foodxml.scripts

import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import scala.xml.XML

import au.com.bytecode.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter

object GuideImageUsage extends App {

  case class Reference(guideId: String, foodName: String, foodCode: String)

  val dataDir = "/home/ivan/Projects/Intake24/intake24-data"

  val foods = FoodDef.parseXml(XML.load(dataDir + "/foods.xml"))

  val refs = foods.flatMap {
    food =>
      food.portionSizeMethods.filter(_.method == "guide-image").map(m => Reference(m.parameters.find(_.name == "guide-image-id").get.value, food.description, food.code))
  }
   
  val usages = refs.groupBy(_.guideId)
  
  val writer = new CSVWriter(new FileWriter(new File("/home/ivan/tmp/guide_image_usage.csv")))
    
  writer.writeNext(Array("Guide image ID", "Used by"))
  
  usages.keySet.toSeq.sorted.foreach {
    code =>
      val u = usages(code)
      writer.writeNext(code +: u.map(_.foodCode).toArray)
  }
  
  writer.close()
}