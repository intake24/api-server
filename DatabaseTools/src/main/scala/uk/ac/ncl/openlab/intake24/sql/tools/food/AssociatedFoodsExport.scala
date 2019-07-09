package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.io.{File, FileWriter}

import com.opencsv.CSVWriter
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.data.admin.{CategoryHeader, FoodHeader}
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodsAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodDataServiceImpl
import uk.ac.ncl.openlab.intake24.sql.tools._

import scala.language.reflectiveCalls

object AssociatedFoodsExport extends App with DatabaseConnection with WarningMessage {

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)
    val outputFile = opt[String](required = true)
    val locale = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())

  val dataSource = getDataSource(databaseConfig)

  val indexService = new FoodIndexDataImpl(dataSource)
  val foodAdminService = new FoodsAdminImpl(dataSource)
  val foodDataService = new FoodDataServiceImpl(dataSource)

  val csvWriter = new CSVWriter(new FileWriter(new File(options.outputFile())))

  csvWriter.writeNext(Array("Intake24 code", "English description", "Local description", "Associated food code", "Associated food name",
    "Associated category code", "Associated category name", "Prompt text", "Link as main food", "Generic name"))

  indexService.indexableFoods(options.locale()) match {
    case Right(foods) =>
      foods.sortBy(_.code).foreach {
        header =>

          println(header.code)

          val foodRecord = foodAdminService.getFoodRecord(header.code, options.locale()).right.get

          val foodEnglishDescription = foodRecord.main.englishDescription
          val associatedFoods = foodRecord.local.associatedFoods

          if (associatedFoods.nonEmpty) {

            associatedFoods.foreach {
              af =>

                af.foodOrCategoryHeader match {
                  case Left(FoodHeader(code, englishDescription, localDescription, excluded)) =>
                    csvWriter.writeNext(Array(header.code, foodEnglishDescription, header.localDescription, code, englishDescription, "", "", af.promptText, af.linkAsMain.toString, af.genericName))
                  case Right(CategoryHeader(code, englishDescription, localDescription, isHidden)) =>
                    csvWriter.writeNext(Array(header.code, foodEnglishDescription, header.localDescription, "", "", code, englishDescription, af.promptText, af.linkAsMain.toString, af.genericName))
                }
            }
          }
      }

  }

  csvWriter.close()

  println("Done!")
}
