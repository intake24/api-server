package uk.ac.ncl.openlab.intake24.sql.tools.food

import java.io.{File, FileWriter}

import com.opencsv.CSVWriter
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodsAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.{FoodBrowsingServiceImpl, FoodDataServiceImpl}
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

  val foodBrowsingService = new FoodBrowsingServiceImpl(dataSource)

  val csvWriter = new CSVWriter(new FileWriter(new File(options.outputFile())))

  csvWriter.writeNext(Array("Intake24 code", "English description", "Local description",
    "Food composition table", "Food composition record ID",
    "Ready Meal Option", "Same As Before Option", "Reasonable Amount", "Use In Recipes",
    "Associated Food / Category", "Brand Names", "Portion size estimation methods", "Categories"))

  indexService.indexableFoods(options.locale()) match {
    case Right(foods) =>
      foods.sortBy(_.code).foreach {
        header =>

          println(header.code)

          foodsService.getFoodRecord(header.code, options.locale()) match {
            case Right(food) =>

              val (tableIds, recordIds) = food.local.nutrientTableCodes.toArray.unzip

              val categories = foodBrowsingService.getFoodAllCategories(header.code).right.get.toArray.sorted

              val psm = food.local.portionSize.map {
                psm =>
                  (Seq(s"Method: ${psm.method}", s"conversion: ${psm.conversionFactor}") ++ psm.parameters.map(p => s"${p.name}: ${p.value}")).mkString(", ")
              }.mkString("\n")

              val readyMealOption = food.main.attributes.readyMealOption.getOrElse("Inherited").toString
              val sameAsBeforeOption = food.main.attributes.sameAsBeforeOption.getOrElse("Inherited").toString
              val reasonableAmount = food.main.attributes.reasonableAmount.getOrElse("Inherited").toString
              val useInRecipes = food.main.attributes.useInRecipes.getOrElse("Inherited") match {
                case 0 => "Anywhere"
                case 1 => "RegularFoodsOnly"
                case 2 => "RecipesOnly"
                case _ => "Inherited"
              }

              val assocFoods = food.local.associatedFoods.map(_.toAssociatedFood).map(food => food.foodOrCategoryCode.merge).mkString(", ")
              val brandNames = food.local.brandNames.mkString(", ")

              if (food.allowedInLocale(options.locale())) {
                val row =
                  Array(food.main.code, food.main.englishDescription, food.local.localDescription.getOrElse("N/A")) ++
                    Array(tableIds.mkString("\n"), recordIds.mkString("\n")) ++
                    Array(readyMealOption, sameAsBeforeOption, reasonableAmount, useInRecipes,
                      assocFoods, brandNames, psm) ++ categories
                csvWriter.writeNext(row)
              }

            case Left(e) => e.exception.printStackTrace()
          }

      }
    case Left(e) => e.exception.printStackTrace()

  }

  csvWriter.close()

  println("Done!")
}
