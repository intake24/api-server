package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.io.{File, FileWriter}

import com.opencsv.CSVWriter
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodsAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodDataServiceImpl
import uk.ac.ncl.openlab.intake24.sql.tools._

import scala.language.reflectiveCalls

object FoodListExport extends App with DatabaseConnection with WarningMessage {

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)
    val outputFile = opt[String](required = true)
    val locale = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())

  val dataSource = getDataSource(databaseConfig)

  val indexService = new FoodIndexDataImpl(dataSource)
  val foodsService = new FoodsAdminImpl(dataSource)

  val csvWriter = new CSVWriter(new FileWriter(new File(options.outputFile())))

  csvWriter.writeNext(Array("Intake24 code", "English description", "Local description", "Food composition table", "Food composition record ID"))

  indexService.indexableFoods(options.locale()) match {
    case Right(foods) =>
      foods.sortBy(_.code).foreach {
        header =>

          println(header.code)

          foodsService.getFoodRecord(header.code, options.locale()) match {
            case Right(food) =>

              val compTables = food.local.nutrientTableCodes.toArray.flatMap(t => Array(t._1, t._2))

              val row = Array(food.main.code, food.main.englishDescription, food.local.localDescription.getOrElse("N/A")) ++ compTables

              csvWriter.writeNext(row)

            case Left(e) => e.exception.printStackTrace()
          }

      }
    case Left(e) => e.exception.printStackTrace()

  }

  csvWriter.close()

  println("Done!")
}
