package uk.ac.ncl.openlab.intake24.foodxml.scripts

import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import scala.xml.XML
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import au.com.bytecode.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter

object AsServedImageUsage extends App {

  case class Reference(setId: String, foodName: String, foodCode: String)

  val dataDir = "/home/ivan/Projects/Intake24/intake24-data"

  val foods = FoodDef.parseXml(XML.load(dataDir + "/foods.xml"))

  val refs = foods.flatMap {
    food =>
      val as = food.localData.portionSize.filter(_.method == "as-served")

      as.flatMap {
        m =>
          val serving = m.parameters.find(_.name == "serving-image-set").map(p => Reference(p.value, food.englishDescription, food.code))
          val leftovers = m.parameters.find(_.name == "leftovers-image-set").map(p => Reference(p.value, food.englishDescription, food.code))
          Seq(serving, leftovers).flatten
      }
  }

  val usages = refs.groupBy(_.setId)

  val writer = new CSVWriter(new FileWriter(new File("/home/ivan/tmp/asserved_image_usage.csv")))

  writer.writeNext(Array("As served image set ID", "Used by"))

  usages.keySet.toSeq.sorted.foreach {
    code =>
      val u = usages(code)
      writer.writeNext(code +: u.map(_.foodCode).toArray)
  }

  writer.close()
}