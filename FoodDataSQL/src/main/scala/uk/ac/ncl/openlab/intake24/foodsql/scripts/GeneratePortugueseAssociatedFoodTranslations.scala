package uk.ac.ncl.openlab.intake24.foodsql.scripts

import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl

import java.io.FileWriter
import au.com.bytecode.opencsv.CSVWriter

import scala.collection.JavaConversions._
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodDataUserImpl

object GeneratePortugueseAssociatedFoodTranslations extends App with DatabaseScript {
  
  val portugueseLocaleCode = "pt_PT"
  val baseLocaleCode = "en_GB"
  
  val dataSource = getLocalDataSource("intake24_foods_development")
  
  val dataService = new FoodDatabaseImpl(dataSource)
  val indexService = new FoodIndexDataImpl(dataSource)
  
  val baseLocaleFoods = indexService.indexableFoods(baseLocaleCode).right.get.map( header => header.code -> header).toMap
  
  val csvRows = indexService.indexableFoods(portugueseLocaleCode).right.get.flatMap {
    foodHeader =>
      dataService.associatedFoods(foodHeader.code, baseLocaleCode) match {
        case Right(prompts) => prompts.toArray.map {
          prompt =>
            val assocFood = prompt.foodOrCategoryCode.left.toOption.getOrElse("")
            val assocCategory = prompt.foodOrCategoryCode.right.toOption.getOrElse("")
            
            Array(foodHeader.code, assocFood, assocCategory, prompt.linkAsMain.toString, baseLocaleFoods(foodHeader.code).localDescription, foodHeader.localDescription, prompt.promptText, "", prompt.genericName, "")
        }
        
        case _ => throw new RuntimeException("Failed to associated foods data for " + foodHeader.code)
      }
      
  }.filterNot(_.isEmpty)
  
  val csv = new CSVWriter(new FileWriter("/home/ivan/Projects/Intake24/recoding/Portuguese/associated_food_prompts_010716.csv"))
    
  csv.writeNext(Array("Food code", "Associated food", "Associated category", "Link as main", "English food description", "Portuguese food description", "English prompt text", "Portuguese prompt text", "English generic food name", "Portuguese generic food name"))
  csv.writeAll(csvRows)
  csv.close()
    
}
