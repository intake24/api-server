package uk.ac.ncl.openlab.intake24.foodxml.scripts

import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import scala.xml.XML
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import au.com.bytecode.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter

object StandardiseUnitsPrepare extends App {

  case class UnitReference(unitName: String, foodName: String, foodCode: String)

  val dataDir = "/home/ivan/Projects/Intake24/intake24-data"

  val foods = FoodDef.parseXml(XML.load(dataDir + "/foods.xml"))

  def parseUnits(params: Seq[PortionSizeMethodParameter]): Seq[String] = {
    val count = params.find(_.name == "units-count").get.value.toInt

    Range(0, count).map {
      index => params.find(_.name == s"unit$index-name").get.value
    }
  }

  val unitRefs = foods.flatMap {
    food =>
      food.local.portionSize.filter(_.method == "standard-portion").flatMap(x => parseUnits(x.parameters).map(UnitReference(_, food.main.englishDescription, food.main.code)))
  }
   
  val unitUsages = unitRefs.groupBy(_.unitName)
  
  val writer = new CSVWriter(new FileWriter(new File("/home/ivan/tmp/units.csv")))
  
  writer.writeNext(Array("Total number of unit definitions", unitRefs.length.toString()))
  
  writer.writeNext(Array("Number of unique unit definitions", unitUsages.size.toString()))

  writer.writeNext(Array("Unit name", "Used by"))
  
  unitUsages.keySet.toSeq.sorted.foreach {
    code =>
      val usages = unitUsages(code)
      writer.writeNext(code +: usages.map(s => s"${s.foodCode} (${s.foodName})").toArray)
  }
  
  writer.close()
}
