package uk.ac.ncl.openlab.intake24.foodxml.scripts

import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import scala.xml.XML
import net.scran24.fooddef.PortionSizeMethodParameter
import au.com.bytecode.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import uk.ac.ncl.openlab.intake24.foodxml.Util
import java.io.PrintWriter

object StandardisePsmDescriptionsTemplate extends App {

  val reader = new CSVReader(new FileReader("/home/ivan/tmp/psm.csv"))
  
  val rows = reader.readAll().tail.filter(row => !row(2).isEmpty())
  
  val outRows = rows.map {
    row =>row(0).toLowerCase().replace(" ", "_") -> row(0)
  }.toMap
 
  
   //val writer = new PrintWriter("/home/ivan/tmp/psm.properties")
  
  outRows.keySet.toSeq.sorted.foreach {
    key =>
      println (s"""Description("${outRows(key)}", "$key"),""")
  }

  //writer.close()
}