package uk.ac.ncl.openlab.intake24.sql.tools.food

import org.apache.poi.ss.usermodel._
import org.apache.poi.util.Units
import org.apache.poi.xssf.usermodel._
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.admin.{AsServedSetsAdminImpl, GuideImageAdminImpl}
import uk.ac.ncl.openlab.intake24.foodsql.user._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedSetWithPaths
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{UserGuideImage, UserImageMap}
import uk.ac.ncl.openlab.intake24.sql.tools._

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.Path
import scala.collection.mutable
import scala.language.reflectiveCalls


object ExportImageGallerySpreadsheet extends App with DatabaseConnection with WarningMessage {

  val options = new ScallopConf(args) with DatabaseConfigurationOptions {
    val imageDir = opt[String](required = true)
    val out = opt[String](required = true)
  }
  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())
  val dataSource = getDataSource(databaseConfig)

  val asServedService = new AsServedSetsServiceImpl(dataSource)
  val asServedAdminService = new AsServedSetsAdminImpl(dataSource, asServedService)


  val guideImageService = new GuideImageServiceImpl(dataSource)
  val guideImageAdminService = new GuideImageAdminImpl(dataSource, guideImageService, null, null, null)

  val imageMapService = new ImageMapServiceImpl(dataSource)

  val pictureIdCache = mutable.HashMap[String, Int]()

  case class PSMRowData(method: String, id: String, image: Option[Path], info: Option[XSSFRichTextString])


  def appendAsServedSetRows(workbook: XSSFWorkbook, sheet: XSSFSheet, drawing: XSSFDrawing, helper: XSSFCreationHelper,
                            set: AsServedSetWithPaths, cellStyle: CellStyle): Unit = {

    println(s"Writing ${set.description}")

    val rowNum = sheet.getLastRowNum() + 1

    val row = sheet.createRow(rowNum)
    row.setHeightInPoints(200)
    row.setRowStyle(cellStyle)

    val row2 = sheet.createRow(rowNum + 1)
    row.setRowStyle(cellStyle)

    val cs = workbook.createCellStyle()
    cs.cloneStyleFrom(cellStyle)
    cs.setWrapText(true)

    val weightCs = workbook.createCellStyle()
    weightCs.cloneStyleFrom(cellStyle)
    weightCs.setAlignment(HorizontalAlignment.CENTER)

    val setIdCell = row.createCell(0)
    setIdCell.setCellStyle(cs)
    setIdCell.setCellValue(set.id)

    val setDescCell = row.createCell(1)
    setDescCell.setCellStyle(cs)
    setDescCell.setCellValue(set.description)

    set.images.zipWithIndex.foreach {
      case (image, index) =>

        row.createCell(index + 2)

        val pictureIndex = pictureIdCache.getOrElseUpdate(image.imagePath, {
          val is = new FileInputStream(options.imageDir() + File.separator + image.imagePath)
          val id = workbook.addPicture(is, Workbook.PICTURE_TYPE_JPEG)
          is.close()
          id
        })

        val anchor = helper.createClientAnchor()
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE)

        anchor.setRow1(rowNum)
        anchor.setRow2(rowNum + 1)
        anchor.setCol1(index + 2)
        anchor.setCol2(index + 3)

        anchor.setDx1(4 * Units.EMU_PER_PIXEL)
        anchor.setDy1(4 * Units.EMU_PER_PIXEL)
        anchor.setDx2(-4 * Units.EMU_PER_PIXEL)
        anchor.setDy2(-4 * Units.EMU_PER_PIXEL)

        drawing.createPicture(anchor, pictureIndex)

        val weightCell = row2.createCell(index + 2)
        weightCell.setCellValue(s"${image.weight} g")
        weightCell.setCellStyle(weightCs)
    }
  }


  def appendGuideImageRows(workbook: XSSFWorkbook, sheet: XSSFSheet, drawing: XSSFDrawing, helper: XSSFCreationHelper, guideImageId: String,
                           guideImage: UserGuideImage,
                           imageMap: UserImageMap, cellStyle: CellStyle): Unit = {

    println(s"Writing ${guideImage.description}")

    val rowNum = sheet.getLastRowNum() + 1

    val row = sheet.createRow(rowNum)
    row.setHeightInPoints(200)
    row.setRowStyle(cellStyle)

    val cs = workbook.createCellStyle()
    cs.cloneStyleFrom(cellStyle)
    cs.setWrapText(true)

    val idCell = row.createCell(0)
    idCell.setCellStyle(cs)
    idCell.setCellValue(guideImageId)

    val imageCell = row.createCell(1)
    imageCell.setCellStyle(cs)

    val weightCell = row.createCell(2)
    weightCell.setCellStyle(cs)

    val weightsString = imageMap.objects.map {
      obj =>
        s"${obj.id} (${obj.description}): ${guideImage.weights.getOrElse(obj.id, 0.0)} g"
    }.mkString(", ")

    weightCell.setCellValue(weightsString)

    val pictureIndex = pictureIdCache.getOrElseUpdate(imageMap.baseImagePath, {
      val is = new FileInputStream(options.imageDir() + File.separator + imageMap.baseImagePath)
      val id = workbook.addPicture(is, Workbook.PICTURE_TYPE_JPEG)
      is.close()
      id
    })

    val anchor = helper.createClientAnchor()
    anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE)

    anchor.setRow1(rowNum)
    anchor.setRow2(rowNum + 1)
    anchor.setCol1(1)
    anchor.setCol2(2)

    anchor.setDx1(4 * Units.EMU_PER_PIXEL)
    anchor.setDy1(4 * Units.EMU_PER_PIXEL)
    anchor.setDx2(-4 * Units.EMU_PER_PIXEL)
    anchor.setDy2(-4 * Units.EMU_PER_PIXEL)

    drawing.createPicture(anchor, pictureIndex)
  }


  def writeXlsx(asServedSets: Seq[AsServedSetWithPaths], guideImages: Seq[(String, UserGuideImage, UserImageMap)]): Unit = {

    val workbook = new XSSFWorkbook()
    val helper = workbook.getCreationHelper()
    val asServedSheet = workbook.createSheet("As served")
    val guideSheet = workbook.createSheet("Guide images")

    val headerStyle = workbook.createCellStyle()
    val headerFont = workbook.createFont()

    headerFont.setBold(true)
    headerStyle.setFont(headerFont)
    headerStyle.setBorderBottom(BorderStyle.THIN)

    val cellStyle = workbook.createCellStyle()
    cellStyle.setVerticalAlignment(VerticalAlignment.TOP)

    val headerRow = asServedSheet.createRow(0)
    headerRow.setRowStyle(headerStyle)

    val setIdCell = headerRow.createCell(0)
    setIdCell.setCellValue("Set ID")
    setIdCell.setCellStyle(headerStyle)

    val setDescCell = headerRow.createCell(1)
    setDescCell.setCellValue("Set description")
    setDescCell.setCellStyle(headerStyle)

    val guideHeaderRow = guideSheet.createRow(0)
    guideHeaderRow.setRowStyle(headerStyle)

    val guideIdCell = guideHeaderRow.createCell(0)
    guideIdCell.setCellStyle(headerStyle)
    guideIdCell.setCellValue("Guide image id")

    val guideImageCell = guideHeaderRow.createCell(1)
    guideImageCell.setCellStyle(headerStyle)
    guideImageCell.setCellValue("Guide image")

    guideSheet.setColumnWidth(0, 30 * 256)
    guideSheet.setColumnWidth(1, 57 * 256)
    guideSheet.setColumnWidth(2, 50 * 256)

    Range.inclusive(2, 11).foreach {
      i =>
        val imageCell = headerRow.createCell(i)
        imageCell.setCellValue(s"Image ${i - 1}")
        imageCell.setCellStyle(headerStyle)
        asServedSheet.setColumnWidth(i, 57 * 256)
    }

    asServedSheet.setColumnWidth(0, 30 * 256)
    asServedSheet.setColumnWidth(1, 50 * 256)

    val drawing = asServedSheet.createDrawingPatriarch()
    val guideDrawing = guideSheet.createDrawingPatriarch()

    asServedSets.foreach {
      set =>
        appendAsServedSetRows(workbook, asServedSheet, drawing, helper, set, cellStyle)
    }

    guideImages.foreach {
      case (id, guideImage, imageMap) =>
        appendGuideImageRows(workbook, guideSheet, guideDrawing, helper, id, guideImage, imageMap, cellStyle)
    }


    val outputStream = new FileOutputStream(options.out())
    workbook.write(outputStream)
    outputStream.close()
  }

  val asServedSetIds = asServedAdminService.listAsServedSets().right.get.keySet.toSeq

  val asServedSets = asServedSetIds.map {
    id =>
      asServedAdminService.getAsServedSetWithPaths(id).right.get
  }.sortBy(_.description)

  val guideImageIds = guideImageAdminService.listGuideImages().right.get.map(_.id)

  val guideImages = guideImageIds.map {
    id =>
      val guideImage = guideImageService.getGuideImage(id).right.get
      val imageMap = imageMapService.getImageMap(guideImage.imageMapId).right.get

      (id, guideImage, imageMap)

  }

  writeXlsx(asServedSets, guideImages)
}
