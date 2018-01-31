package uk.ac.ncl.openlab.intake24.foodxml.scripts

import java.io.{File, FileInputStream}
import java.util.Properties

import uk.ac.ncl.openlab.intake24.api.data.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef

import scala.xml.XML

object StandardiseUnitsVerify extends App {

  val dataDir = "/home/ivan/Projects/Intake24/intake24-data"

  val foods = FoodDef.parseXml(XML.load(dataDir + "/foods.xml"))

  val props = new Properties()

  props.load(new FileInputStream(new File("/home/ivan/Projects/Intake24/intake24/Webapp/src/main/java/net/scran24/user/client/survey/StandardUnits_en.properties")))

  def parseUnits(params: Seq[PortionSizeMethodParameter]): Seq[String] = {
    val count = params.find(_.name == "units-count").get.value.toInt

    Range(0, count).map {
      index => params.find(_.name == s"unit$index-name").get.value
    }
  }

  val standardPsm = foods.flatMap {
    food =>
      food.portionSizeMethods.filter(_.method == "standard-portion")
  }

  standardPsm.foreach {
    m =>
      val units = parseUnits(m.parameters)

      val keys = units.map(_ + "_estimate_in") ++ units.map(_ + "_how_many")

      keys.foreach {
        key =>
          if (props.getProperty(key) == null) {
            println(s"Missing property for $key")
          }
      }
  }
}
