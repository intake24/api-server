package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.io.{File, FileWriter, PrintWriter}

import com.opencsv.CSVWriter
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.data.admin.CategoryHeader
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodBrowsingAdminImpl
import uk.ac.ncl.openlab.intake24.sql.tools._

import scala.language.reflectiveCalls

object CategoryListExport extends App with DatabaseConnection with WarningMessage {

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)
    val outputFile = opt[String](required = true)
    val locale = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())

  val dataSource = getDataSource(databaseConfig)

  val foodBrowsingService = new FoodBrowsingAdminImpl(dataSource)

  val writer = new PrintWriter(new File(options.outputFile()))

  val locale = options.locale()

  def writeCategory(header: CategoryHeader, offset: Int): Unit = {

    val strOffset = Array.fill(offset)("  ").mkString("")

    if (locale == "en_GB")
      writer.println(strOffset + s"<li><strong>${header.code}</strong> ${header.englishDescription}</li>")
    else
      writer.println(strOffset + s"<li><strong>${header.code}</strong> ${header.englishDescription} (${header.localDescription.getOrElse("Local name missing")})</li>")

    val children = foodBrowsingService.getCategoryContents(header.code, locale).right.get

    if (children.subcategories.nonEmpty)
      writer.println(strOffset + "<ul>")

    children.subcategories.foreach {
      subcategory =>
        writeCategory(subcategory, offset + 1)
    }

    if (children.subcategories.nonEmpty)
      writer.println(strOffset + "</ul>")
  }


  val rootCategories = foodBrowsingService.getRootCategories(options.locale()).right.get

  writer.println("<ul>")

  rootCategories.zipWithIndex.foreach {
    case (header, index) =>

      println(s"Processing $index out of ${rootCategories.size}: [${header.code}] ${header.englishDescription}")
      writeCategory(header, 1)
  }

  writer.println("</ul>")


  writer.close()

  println("Done!")
}
