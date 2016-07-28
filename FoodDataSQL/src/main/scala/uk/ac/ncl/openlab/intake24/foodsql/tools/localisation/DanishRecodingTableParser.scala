package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import java.io.File

import scala.collection.JavaConverters.asScalaIteratorConverter

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class DanishRecodingTableParser extends RecodingTableParser with XSSFCellColorHack {

  val numberOfHeaderRows = 1
  val recodingSheetNumber = 0

  val englishDecriptionCellIndex = 6
  val localDescriptionCellIndex = 5
  val foodTableRecordCellIndex = 4

  // This seems horribly fragile; in the future, just use CSV (i.e., an explicit column) for this sort of work 
  
  val useUKColor = "FA96DC"
  val useDKColor = "FFFFFF" // LibreOffice files return expected value for while fill
  val useDKColor2 = "000000" // But Excel files return 000000 ???
  val doNotUseColor = "FAC090"
  val doNotUseColor2 = "FABF8F"
  val useNewColor = "BFBFBF"

  val dataFormatter = new DataFormatter()

  def parseUseUK(row: Row) =
    UseUKFoodTable(dataFormatter.formatCellValue(row.getCell(localDescriptionCellIndex)))

  def parseUseDK(row: Row) =
    UseLocalFoodTable(
      dataFormatter.formatCellValue(row.getCell(localDescriptionCellIndex)),
      dataFormatter.formatCellValue(row.getCell(foodTableRecordCellIndex)))

  def parseUseNew(row: Row) =
    NewFoodRecord(
      dataFormatter.formatCellValue(row.getCell(englishDecriptionCellIndex)),
      dataFormatter.formatCellValue(row.getCell(localDescriptionCellIndex)),
      dataFormatter.formatCellValue(row.getCell(foodTableRecordCellIndex)))

  def parseTable(path: String): RecodingTable = {
    val pkg = OPCPackage.open(new File(path))
    val wb = new XSSFWorkbook(pkg)

    val sheet = wb.getSheetAt(recodingSheetNumber)

    val z = RecodingTable(Map(), Seq())

    val result = sheet.rowIterator().asScala.drop(numberOfHeaderRows).foldLeft(z) {
      (table, row) =>

        val code = dataFormatter.formatCellValue(row.getCell(0))

        val cellForColor = row.getCell(row.getFirstCellNum()).asInstanceOf[XSSFCell]
        val colorHex = getCellColor(wb, cellForColor, useDKColor)

        colorHex match {
          case `useUKColor` => table.copy(existingFoodsCoding = table.existingFoodsCoding + (code -> parseUseUK(row)))
          case `useDKColor` | `useDKColor2` => table.copy(existingFoodsCoding = table.existingFoodsCoding + (code -> parseUseDK(row)))
          case `doNotUseColor` | `doNotUseColor2` => table.copy(existingFoodsCoding = table.existingFoodsCoding + (code -> DoNotUse))
          case `useNewColor` => table.copy(newFoods = table.newFoods :+ parseUseNew(row))
          case _ => throw new RuntimeException(s"Unexpected cell color: $colorHex. There seem to be rounding issues in Apache POI, please adjust the color constants if that is the case.")
        }
    }

    pkg.close()

    result
  }
}
