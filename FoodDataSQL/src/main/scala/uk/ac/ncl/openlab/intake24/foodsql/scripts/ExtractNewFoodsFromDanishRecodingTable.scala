package uk.ac.ncl.openlab.intake24.foodsql.scripts

import org.apache.poi.openxml4j.opc.OPCPackage
import java.io.File
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scala.collection.JavaConverters._
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.ss.usermodel.DataFormatter
import uk.ac.ncl.openlab.intake24.foodsql.tools.localisation.XSSFCellColorHack
import au.com.bytecode.opencsv.CSVWriter
import java.io.FileWriter
import uk.ac.ncl.openlab.intake24.foodsql.tools.localisation.DanishRecodingTableParser

object ExtractNewFoodsFromDanishRecodingTable extends App  {
  
  val parser = new DanishRecodingTableParser()

  val table = parser.parseTable("/home/ivan/Projects/Intake24/NoHoW/Denmark/Denmark Recoding of foods.xlsx")
  
  val writer = new CSVWriter(new FileWriter("/home/ivan/tmp/new_dk_foods.csv"))
  
  writer.writeNext(Array("Intake24 code", "English description", "Local description", "Local food composition table code", "Intake24 categories"))
  
  table.newFoods.foreach {
    r =>
      writer.writeNext(Array("", r.englishDescription, r.localDescription, r.localTableRecordId, ""))
  }

  writer.close()
}