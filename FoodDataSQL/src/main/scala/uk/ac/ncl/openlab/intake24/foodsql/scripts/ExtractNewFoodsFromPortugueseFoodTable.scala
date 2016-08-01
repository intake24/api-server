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

object ExtractNewFoodsFromPortugueseFoodTable extends App with XSSFCellColorHack {

  val sheetNumber = 0
  val numberOfHeaderRows = 3

  val useColor = "4BACC6"

  case class NewFoodRecord(englishDescription: String, localDescription: String, localCode: String)

  def parseTable(path: String): Seq[NewFoodRecord] = {
    val pkg = OPCPackage.open(new File(path))
    val wb = new XSSFWorkbook(pkg)

    val sheet = wb.getSheetAt(sheetNumber)

    val z = Vector[NewFoodRecord]()

    val dataFormatter = new DataFormatter()

    val result = sheet.rowIterator().asScala.drop(numberOfHeaderRows).foldLeft(z) {
      (seq, row) =>

        val cellForColor = row.getCell(row.getFirstCellNum()).asInstanceOf[XSSFCell]
        val colorHex = getCellColor(wb, cellForColor, "HAHAHA")

        if (colorHex == useColor)
          seq :+ NewFoodRecord(dataFormatter.formatCellValue(row.getCell(3)), dataFormatter.formatCellValue(row.getCell(2)), dataFormatter.formatCellValue(row.getCell(0)))
        else
          seq
    }

    pkg.close()
    
    result
  }

  val records = parseTable("/home/ivan/Projects/Intake24/NoHoW/Portugal/Portuguese Food Composition Table INSA en.xlsx")
  
  val writer = new CSVWriter(new FileWriter("/home/ivan/tmp/new_pt_foods.csv"))
  
  writer.writeNext(Array("Intake24 code", "English description", "Local description", "Local food composition table code", "Intake24 categories"))
  
  records.foreach {
    r =>
      writer.writeNext(Array("", r.englishDescription, r.localDescription, r.localCode, ""))
  }

  writer.close()
}