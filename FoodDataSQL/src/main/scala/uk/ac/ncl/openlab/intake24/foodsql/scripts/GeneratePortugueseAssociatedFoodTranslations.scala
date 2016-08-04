package uk.ac.ncl.openlab.intake24.foodsql.scripts

import uk.ac.ncl.openlab.intake24.foodsql.admin.AdminFoodDataServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.admin.AdminFoodDataServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.IndexFoodDataServiceSqlImpl
import uk.ac.ncl.openlab.intake24.foodsql.UserFoodDataServiceSqlImpl
import java.io.FileWriter
import au.com.bytecode.opencsv.CSVWriter

import scala.collection.JavaConversions._

object GeneratePortugueseAssociatedFoodTranslations extends App with DatabaseScript {
  
  val portugueseLocaleCode = "pt_PT"
  val baseLocaleCode = "en_GB"
  
  val dataSource = getLocalDataSource("intake24_foods_development")
  
  val dataService = new UserFoodDataServiceSqlImpl(dataSource)
  val indexService = new IndexFoodDataServiceSqlImpl(dataSource)
  
  val baseLocaleFoods = indexService.indexableFoods(baseLocaleCode).map( header => header.code -> header).toMap
  
  val csvRows = indexService.indexableFoods(portugueseLocaleCode).flatMap {
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
