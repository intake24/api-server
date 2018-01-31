package uk.ac.ncl.openlab.intake24.foodxml.scripts

import java.io.{File, FileReader, FileWriter, PrintWriter}

import au.com.bytecode.opencsv.{CSVReader, CSVWriter}
import uk.ac.ncl.openlab.intake24.api.data.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.foodxml.{FoodDef, Util}

import scala.collection.JavaConverters._
import scala.xml.XML

object StandardiseUnitsApply extends App {

  case class UnitReference(unitName: String, foodName: String, foodCode: String)

  case class StandardUnit(description: String, omitFoodDescription: Boolean, weight: Double)

  case class ReplacementUnit(unit_id: String, omitFoodDescription: Boolean)

  val dataDir = "/home/ivan/Projects/Intake24/intake24-data"

  val csvPath = "/home/ivan/tmp/stdunits.csv"

  val foods = FoodDef.parseXml(XML.load(dataDir + "/foods.xml"))

  val reader = new CSVReader(new FileReader(new File(csvPath)))


  def convertToIdentifier(s: String) = s.replaceAll(" ", "_").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\/", "_").replaceAll("-", "_").replaceAll("^1", "one")

  val unitMap = reader.readAll().asScala.foldLeft(Map[String, ReplacementUnit]()) {
    (acc, row) =>
      if (row(1).isEmpty)
        acc
      else
        acc + (row(0) -> ReplacementUnit(row(1), row(2) == "Y"))
  }.toMap

  reader.close()


  val unique_ids = unitMap.values.toSeq.map(_.unit_id).distinct

  val w1 = new CSVWriter(new FileWriter(new File("/home/ivan/tmp/standard_unit_translation.csv")))
  val w2 = new PrintWriter(new File("/home/ivan/tmp/standard_unit_java_template.java"));
  val w3 = new PrintWriter(new File("/home/ivan/tmp/standard_unit_properties.properties"));

  w1.writeNext(Array("Unit identifier", "English unit name", "Translated unit name, locative", "Translated unit name, genitive"))
  w1.writeNext(Array("", "", "E.g.: estimate in small bags", "E.g.: how many small bags"))

  unique_ids.sorted.foreach {
    x =>
      val id = convertToIdentifier(x)
      w1.writeNext(Array(id, x, "", ""))
      w2.println(s"public String ${id}_estimate_in();")
      w2.println(s"public String ${id}_how_many();")
      w2.println()
      w3.println(s"${id}_estimate_in = $x")
      w3.println(s"${id}_how_many = How many $x")
      w3.println()
  }

  w1.close()
  w2.close()
  w3.close()

  def replaceUnits(params: Seq[PortionSizeMethodParameter]) = {
    val count = params.find(_.name == "units-count").get.value.toInt

    val oldUnits = Range(0, count).map {
      index =>
        val desc = params.find(_.name == s"unit$index-name").get.value
        val omit = params.find(_.name == s"unit$index-omit-food-description").get.value.toBoolean
        val weight = params.find(_.name == s"unit$index-weight").get.value.toDouble
        StandardUnit(desc, omit, weight)
    }

    val newUnits = oldUnits.flatMap {
      unit =>
        unitMap.get(unit.description).map {
          repl =>
            StandardUnit(convertToIdentifier(repl.unit_id), repl.omitFoodDescription, unit.weight)
        }
    }

    val newCount = newUnits.size

    PortionSizeMethodParameter("units-count", newCount.toString) +:
      newUnits.zipWithIndex.flatMap {
        case (unit, index) => Seq(
          PortionSizeMethodParameter(s"unit$index-name", unit.description),
          PortionSizeMethodParameter(s"unit$index-omit-food-description", unit.omitFoodDescription.toString),
          PortionSizeMethodParameter(s"unit$index-weight", unit.weight.toString))
      }
  }

  val newFoods = foods.map {
    food =>
      val newPsm = food.portionSizeMethods.flatMap {
        psm =>
          psm.method match {
            case "standard-portion" => {
              val newParams = replaceUnits(psm.parameters)
              if (newParams.find(_.name == "units-count").get.value.toInt == 0)
                None
              else
                Some(psm.copy(parameters = newParams))
            }
            case _ => Some(psm)
          }
      }
      food.copy(portionSizeMethods = newPsm)
  }

  Util.writeXml(FoodDef.toXml(newFoods), dataDir + "/foods-new.xml")

}
