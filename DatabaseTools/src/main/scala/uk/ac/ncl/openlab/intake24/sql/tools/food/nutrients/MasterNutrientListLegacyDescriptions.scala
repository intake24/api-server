package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import java.io.FileReader

import au.com.bytecode.opencsv.CSVReader
import org.rogach.scallop.ScallopConf

import scala.collection.JavaConverters._

object MasterNutrientListLegacyDescriptions extends App {

  val options = new ScallopConf(args) {
    val nutrientsList = opt[String](required = true)
  }

  private case class Row(index: Long, description: String, unit: String, ukCol: String)

  options.verify()

  println("Parsing nutrient list CSV...")

  val reader = new CSVReader(new FileReader(options.nutrientsList()))

  val lines = reader.readAll().asScala.toIndexedSeq

  reader.close()

  val nutrientNames = lines(0).tail.map(_.trim())
  val units = lines(1).tail.map(_.trim())
  val supportedUk = lines(2).tail.map(_.trim())

  private val rows = nutrientNames.zip(supportedUk).zip(units).zipWithIndex.map {
    case (((desc, ukCol), unit), index) => Row(index + 1, desc, unit, ukCol)
  }

  rows.filterNot(_.ukCol.isEmpty).foreach {
    row =>
      println(s"""NutrientDescription(${row.index}, "${row.description}", "${row.unit}"),""")
  }


}
