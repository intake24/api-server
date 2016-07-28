package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFCell

trait XSSFCellColorHack {
   def getCellColor(wb: XSSFWorkbook, cell: XSSFCell, default: String): String = {

    val style = cell.getCellStyle()

    val fgColorFromCell = style.getFillForegroundColorColor()

    val fgColor =
      if (fgColorFromCell != null)
        fgColorFromCell
      else {
        // This a completely fucked up workaround for getFillForegroundColorColor returning null
        // see http://stackoverflow.com/questions/26675062/poi-excel-get-style-name

        val fillId = style.getCoreXf().getFillId().toShort
        val fill = wb.getStylesSource().getFillAt(fillId)
        fill.getFillForegroundColor()
      }

    if (fgColor == null)
      default
    else
      fgColor.getRgbWithTint().map("%02X".format(_)).mkString
  }
}
