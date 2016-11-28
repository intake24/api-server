package uk.ac.ncl.openlab.intake24.sql.tools.food.nutrients

import java.io.FileReader

import au.com.bytecode.opencsv.CSVReader
import org.rogach.scallop.ScallopConf

import scala.collection.JavaConverters._

abstract class GenerateCsvMappingFromMasterList(rowOffset: Int) extends App {

  val options = new ScallopConf(args) {
    val nutrientsList = opt[String](required = true)
  }

  private case class Row(index: Long, description: String, unit: String, col: String)

  options.verify()

  println("Parsing nutrient list CSV...")

  val reader = new CSVReader(new FileReader(options.nutrientsList()))

  val lines = reader.readAll().asScala.toIndexedSeq

  reader.close()

  val nutrientNames = lines(0).tail.map(_.trim())
  val units = lines(1).tail.map(_.trim())
  val localColNames = lines(rowOffset).tail.map(_.trim())

  private val rows = nutrientNames.zip(localColNames).zip(units).zipWithIndex.map {
    case (((desc, col), unit), index) => Row(index + 1, desc, unit, col)
  }

  rows.filterNot(_.col.isEmpty).foreach {
    row =>
      println(s"""${row.index}l -> col("${row.col}"),""")
  }
}
