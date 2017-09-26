package uk.ac.ncl.openlab.intake24.foodxml.scripts

import java.io.{File, FileReader, PrintWriter}

import au.com.bytecode.opencsv.CSVReader

import scala.collection.JavaConverters._

object StandardiseUnitsPortuguese extends App {

  val csvPath = "/home/ivan/tmp/pt_std_units.csv"
  val outPath = "/home/ivan/tmp/StandardUnits_pt.properties"

  val reader = new CSVReader(new FileReader(new File(csvPath)))

  val writer = new PrintWriter(new File(outPath))

  case class Row(unit_id: String, estimate_in: String, how_many: String)

  val rows = reader.readAll().asScala.tail.map {
    row => Row(row(0), row(2), row(4))
  }

  rows.foreach {
    row =>

      if (row.estimate_in.nonEmpty && row.how_many.nonEmpty) {

        writer.println(s"${row.unit_id}_estimate_in = ${row.estimate_in}")
        writer.println(s"${row.unit_id}_how_many = ${row.how_many}")
        writer.println()
      }
  }

  reader.close()
  writer.close()
}
