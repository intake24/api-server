package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.io.{File, FileWriter}

import com.opencsv.CSVWriter
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodsAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodDataServiceImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.user.{SourceLocale, SourceRecord}
import uk.ac.ncl.openlab.intake24.sql.tools._

import scala.language.reflectiveCalls

object StandardPortionExport extends App with DatabaseConnection with WarningMessage {

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

  csvWriter.writeNext(Array("Intake24 code", "English description", "Local description", "Standard unit description", "Standard unit weight", "Defined in"))

  indexService.indexableFoods(options.locale()) match {
    case Right(foods) =>
      foods.sortBy(_.code).foreach {
        header =>

          println(header.code)

          val englishDescription = foodAdminService.getFoodRecord(header.code, options.locale()).right.get.main.englishDescription

          val data = foodDataService.getFoodData(header.code, options.locale()).right.get

          val standardPortionMethods = data._1.portionSizeMethods.filter(_.method == "standard-portion")

          if (standardPortionMethods.nonEmpty) {

            standardPortionMethods.foreach {
              psm =>

                val unitCount = psm.parameters.find(_.name == "units-count").get.value.toInt

                (0 to (unitCount - 1)).foreach {
                  i =>
                    val name = psm.parameters.find(_.name == s"unit$i-name").get.value
                    val weight = psm.parameters.find(_.name == s"unit$i-weight").get.value

                    val sourceLocale = data._2.portionSizeSource._1 match {
                      case SourceLocale.Current(code) => code
                      case SourceLocale.Prototype(code) => code
                    }

                    val sourceRecord = data._2.portionSizeSource._2 match {
                      case SourceRecord.FoodRecord(code) => s"food $code"
                      case SourceRecord.CategoryRecord(code) => s"category $code"
                    }


                    csvWriter.writeNext(Array(header.code, englishDescription, header.localDescription, name, weight, s"$sourceLocale $sourceRecord"))

                }


            }

          }
      }

  }

  csvWriter.close()

  println("Done!")
}
