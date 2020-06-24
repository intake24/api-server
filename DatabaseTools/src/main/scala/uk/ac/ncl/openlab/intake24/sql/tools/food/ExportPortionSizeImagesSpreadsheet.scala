package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.io.{FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path, Paths}

import org.apache.poi.ss.usermodel.{BorderStyle, CellStyle, CellType, ClientAnchor, VerticalAlignment, Workbook}
import org.apache.poi.util.Units
import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFCreationHelper, XSSFDrawing, XSSFShape, XSSFSheet, XSSFWorkbook}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.data.{PortionSizeMethod, UserFoodHeader}
import uk.ac.ncl.openlab.intake24.errors.LookupError
import uk.ac.ncl.openlab.intake24.foodsql.admin.{FoodIndexDataAdminImpl, FoodsAdminImpl}
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.{AsServedSetsServiceImpl, DrinkwareServiceImpl, FoodDataServiceImpl, GuideImageServiceImpl, ImageMapServiceImpl}
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{AsServedSetsService, ResolvedFoodData, UserAsServedSet}
import uk.ac.ncl.openlab.intake24.sql.tools._

import scala.collection.mutable
import scala.language.reflectiveCalls


object ExportPortionSizeImagesSpreadsheet extends App with DatabaseConnection with WarningMessage {

  val options = new ScallopConf(args) with DatabaseConfigurationOptions {
    val locale = opt[String](required = true)
    val imageDir = opt[String](required = true)
    val out = opt[String](required = true)
  }
  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())
  val dataSource = getDataSource(databaseConfig)

  val foodsAdminService = new FoodsAdminImpl(dataSource)
  val indexDataService = new FoodIndexDataImpl(dataSource)
  val foodDataService = new FoodDataServiceImpl(dataSource)
  val asServedService = new AsServedSetsServiceImpl(dataSource)
  val guideImageService = new GuideImageServiceImpl(dataSource)
  val imageMapService = new ImageMapServiceImpl(dataSource)

  val drinkwareService = new DrinkwareServiceImpl(dataSource)

  val locale = options.locale()

  val pictureIdCache = mutable.HashMap[String, Int]()

  case class PSMRowData(method: String, id: String, image: Option[Path])

  def getPSMRowData(psm: PortionSizeMethod): PSMRowData =
    psm.method match {
      case "as-served" =>

        val setId = psm.parameters.find(_.name == "serving-image-set").get.value
        println("As served: " + setId)

        val asServedSet = asServedService.getAsServedSet(setId).right.get

        val image = Paths.get(options.imageDir()).resolve(asServedSet.images(asServedSet.images.size / 2).mainImagePath)


        PSMRowData("As served", setId, Some(image))

      case "guide-image" =>

        val guideId = psm.parameters.find(_.name == "guide-image-id").get.value
        println("Guide: " + guideId)

        val guideImage = guideImageService.getGuideImage(guideId).right.get
        val imageMap = imageMapService.getImageMap(guideImage.imageMapId).right.get
        val image = Paths.get(options.imageDir()).resolve(imageMap.baseImagePath)

        PSMRowData("Guide image", guideId, Some(image))

      case "pizza" =>

        val imageMap = imageMapService.getImageMap("gpizza").right.get
        val image = Paths.get(options.imageDir()).resolve(imageMap.baseImagePath)

        PSMRowData("Pizza", "", Some(image))

      case "milk-on-cereal" =>

        PSMRowData("Milk on cereal", "", None)

      case "milk-in-a-hot-drink" =>
        PSMRowData("Milk in a hot drink", "", None)

      case "drink-scale" =>

        val setId = psm.parameters.find(_.name == "drinkware-id").get.value
        val drinkwareSet = drinkwareService.getDrinkwareSet(setId).right.get

        val imageMap = imageMapService.getImageMap(drinkwareSet.guideId).right.get
        val image = Paths.get(options.imageDir()).resolve(imageMap.baseImagePath)

        PSMRowData("Drink scale", setId, Some(image))

      case "cereal" =>
        PSMRowData("Cereal", "", None)

      case "standard-portion" =>
        PSMRowData("Standard portion", "", None)
    }

  def appendPortionSizeMethodRows(workbook: XSSFWorkbook, sheet: XSSFSheet, drawing: XSSFDrawing, helper: XSSFCreationHelper,
                                  food: UserFoodHeader, englishDescription: String, psm: PortionSizeMethod, cellStyle: CellStyle): Unit = {


    println(s"Writing [${food.code}] ${englishDescription}")

    val rowData = getPSMRowData(psm)

    val rowNum = sheet.getLastRowNum() + 1

    val row = sheet.createRow(rowNum)
    row.setRowStyle(cellStyle)

    val codeCell = row.createCell(0)
    val engDescCell = row.createCell(1)
    val localDescCell = row.createCell(2)
    val psmCell = row.createCell(3)
    val psmIdCell = row.createCell(4)
    val psmImageCell = row.createCell(5)

    codeCell.setCellValue(food.code)
    engDescCell.setCellValue(englishDescription)
    localDescCell.setCellValue(food.localDescription)
    psmCell.setCellValue(rowData.method)
    psmIdCell.setCellValue(rowData.id)

    rowData.image match {
      case Some(path) =>

        val pictureIndex = pictureIdCache.getOrElseUpdate(path.toString, {
          val is = new FileInputStream(path.toFile)
          val id = workbook.addPicture(is, Workbook.PICTURE_TYPE_JPEG)
          is.close()
          id
        })


        val anchor = helper.createClientAnchor()
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE)

        anchor.setRow1(rowNum)
        anchor.setRow2(rowNum + 1)
        anchor.setCol1(5)
        anchor.setCol2(6)

        anchor.setDx1(4 * Units.EMU_PER_PIXEL)
        anchor.setDy1(4 * Units.EMU_PER_PIXEL)
        anchor.setDx2(-4 * Units.EMU_PER_PIXEL)
        anchor.setDy2(-4 * Units.EMU_PER_PIXEL)

        val picture = drawing.createPicture(anchor, pictureIndex)


        row.setHeightInPoints(255)

      case None =>
        psmImageCell.setCellValue("")
    }
  }


  def writeXlsx(foods: Seq[UserFoodHeader], foodData: Map[String, ResolvedFoodData]): Unit = {

    val workbook = new XSSFWorkbook()
    val helper = workbook.getCreationHelper()
    val sheet = workbook.createSheet(options.locale())


    val headerStyle = workbook.createCellStyle()
    val headerFont = workbook.createFont()

    headerFont.setBold(true)
    headerStyle.setFont(headerFont)
    headerStyle.setBorderBottom(BorderStyle.THIN)


    val cellStyle = workbook.createCellStyle()
    cellStyle.setVerticalAlignment(VerticalAlignment.TOP)


    val headerRow = sheet.createRow(0)

    val codeHeaderCell = headerRow.createCell(0)
    codeHeaderCell.setCellValue("Intake24 code")

    val engDescHeaderCell = headerRow.createCell(1)
    engDescHeaderCell.setCellValue("Food description (English)")

    val descriptionHeaderCell = headerRow.createCell(2)
    descriptionHeaderCell.setCellValue("Food description (local)")

    val psmHeaderCell = headerRow.createCell(3)
    psmHeaderCell.setCellValue("Portion size method")

    val psmIdHeaderCell = headerRow.createCell(4)
    psmIdHeaderCell.setCellValue("PSM id")

    val psmImageHeaderCell = headerRow.createCell(5)
    psmImageHeaderCell.setCellValue("PSM reference image")

    val psmHeaderCell = headerRow.createCell(6)
    psmImageHeaderCell.setCellValue("PSM reference image")


    headerRow.setRowStyle(headerStyle)

    sheet.setColumnWidth(0, 15 * 256)
    sheet.setColumnWidth(1, 50 * 256)
    sheet.setColumnWidth(2, 50 * 256)
    sheet.setColumnWidth(3, 25 * 256)
    sheet.setColumnWidth(4, 20 * 256)
    sheet.setColumnWidth(5, 70 * 256)


    val drawing = sheet.createDrawingPatriarch()

    val sortedFoods = foods.sortBy(_.code)

    sortedFoods.zipWithIndex.foreach {
      case (header, index) =>
        foodData(header.code).portionSizeMethods.foreach {
          psm =>
            appendPortionSizeMethodRows(workbook, sheet, drawing, helper, header, foodData(header.code).englishDescription, psm, cellStyle)
        }
    }

    val outputStream = new FileOutputStream(options.out())
    workbook.write(outputStream)
    outputStream.close()
  }


  val foods = indexDataService.indexableFoods(options.locale()).right.get

  val foodData = foods.sortBy(_.code).foldLeft(Map[String, ResolvedFoodData]()) {
    (m, header) =>
      println(s"Getting data for ${header.code}")
      val foodData = foodDataService.getFoodData(header.code, options.locale()).right.get._1
      m + (header.code -> foodData)
  }

  writeXlsx(foods, foodData)

}
