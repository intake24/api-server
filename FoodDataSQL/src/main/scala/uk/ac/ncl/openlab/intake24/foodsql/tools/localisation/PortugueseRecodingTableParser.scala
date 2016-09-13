package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import java.io.File

import scala.collection.JavaConverters.asScalaIteratorConverter

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.extensions.XSSFCellFill
import org.apache.poi.xssf.usermodel.extensions.XSSFCellFill
import org.apache.poi.xssf.usermodel.XSSFCell

class PortugueseRecodingTableParser extends RecodingTableParser with XSSFCellColorHack {

  val numberOfHeaderRows = 1
  val recodingSheetNumber = 0

  val englishDecriptionCellIndex = 1
  val localDescriptionCellIndex = 9
  val foodTableRecordCellIndex = 7

  // This seems horribly fragile; in the future, just use CSV (i.e., an explicit column) for this sort of work 
  
  val useUKColor = "C6D9F1"
  val useUKColor2 = "B9CDE5"
  val useUKColor3 = "B8CCE4"
  val usePTColor = "FFFF00" // LibreOffice files return expected value for while fill
  //val usePTColor2 = "BBBBBB" // But Excel files return 000000 ???
  val doNotUseColor = "FFFFFF"
  val doNotUseColor2 = "000000"
  val useNewColor = "F79646"

  val dataFormatter = new DataFormatter()

  def parseUseUK(row: Row) =
    UseUKFoodTable(dataFormatter.formatCellValue(row.getCell(localDescriptionCellIndex)))

  def parseUseLocal(row: Row) =
    UseLocalFoodTable(
      dataFormatter.formatCellValue(row.getCell(localDescriptionCellIndex)),
      dataFormatter.formatCellValue(row.getCell(foodTableRecordCellIndex)))

  def parseUseNew(row: Row) =
    NewFoodRecord(
      dataFormatter.formatCellValue(row.getCell(englishDecriptionCellIndex)),
      dataFormatter.formatCellValue(row.getCell(localDescriptionCellIndex)),
      dataFormatter.formatCellValue(row.getCell(foodTableRecordCellIndex)))

   def parseRecodingTable(path: String): RecodingTable = {
    val pkg = OPCPackage.open(new File(path))
    val wb = new XSSFWorkbook(pkg)

    val sheet = wb.getSheetAt(recodingSheetNumber)

    val z = RecodingTable(Map(), Seq())

    val result = sheet.rowIterator().asScala.drop(numberOfHeaderRows).foldLeft(z) {
      (table, row) =>

        val code = dataFormatter.formatCellValue(row.getCell(0))
        
        // println (code)

        val cellForColor = row.getCell(row.getFirstCellNum()).asInstanceOf[XSSFCell]
        val colorHex = getCellColor(wb, cellForColor, doNotUseColor)

        colorHex match {
          case `useUKColor` | `useUKColor2` | `useUKColor3` => table.copy(existingFoodsCoding = table.existingFoodsCoding + (code -> parseUseUK(row)))
          case `usePTColor` => table.copy(existingFoodsCoding = table.existingFoodsCoding + (code -> parseUseLocal(row)))
          case `doNotUseColor` | `doNotUseColor2` => table.copy(existingFoodsCoding = table.existingFoodsCoding + (code -> DoNotUse))
          case `useNewColor` => table.copy(newFoods = table.newFoods :+ parseUseNew(row))
          case _ => throw new RuntimeException(s"Unexpected cell color: $colorHex. There seem to be rounding issues in Apache POI, please adjust the color constants if that is the case.")
        }
    }

    pkg.close()

    result
  }
}
