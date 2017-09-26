package uk.ac.ncl.openlab.intake24.foodxml.scripts

import java.io.FileReader

import au.com.bytecode.opencsv.CSVReader

import scala.collection.JavaConverters._

object StandardisePsmDescriptionsTemplate extends App {

  val reader = new CSVReader(new FileReader("/home/ivan/tmp/psm.csv"))

  val rows = reader.readAll().asScala.tail.filter(row => !row(2).isEmpty())

  val outRows = rows.map {
    row => row(0).toLowerCase().replace(" ", "_") -> row(0)
  }.toMap


  //val writer = new PrintWriter("/home/ivan/tmp/psm.properties")

  outRows.keySet.toSeq.sorted.foreach {
    key =>
      println(s"""Description("${outRows(key)}", "$key"),""")
  }

  //writer.close()
}