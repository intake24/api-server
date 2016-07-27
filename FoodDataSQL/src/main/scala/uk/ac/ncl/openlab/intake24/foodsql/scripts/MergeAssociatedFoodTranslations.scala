package uk.ac.ncl.openlab.intake24.foodsql.scripts

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import au.com.bytecode.opencsv.CSVWriter
import java.io.FileWriter

object MergeAssociatedFoodTranslations extends App {
  
  val newCsvPath = "/home/ivan/Projects/Intake24/recoding/Portuguese/associated_food_prompts_010716.csv"
  val translatedCsvPath = "/home/ivan/Projects/Intake24/recoding/Portuguese/pt_food_prompts.csv"
  val outputPath = "/home/ivan/Projects/Intake24/recoding/Portuguese/associated_food_prompts_270716.csv"

//  Food code	Associated food or category	Link as main	English food description	Portuguese food description	English prompt text	Portuguese prompt text	English generic food name	Portuguese generic food name
  
  
  case class OldRow(foodCode: String, assocCode: String, englishDesc: String, localDesc: String, englishPrompt: String, localPrompt: String, englishGenericName: String, localGenericName: String)
  case class Row(foodCode: String, assocCode: String, linkAsMain: Boolean, englishDesc: String, localDesc: String, englishPrompt: String, localPrompt: String, englishGenericName: String, localGenericName: String)
  
  val r1 = new CSVReader(new FileReader(translatedCsvPath))
  
  val translated = r1.readAll.tail.map {
    row =>
      (row(0), row(1)) -> OldRow(row(0), row(1), row(2), row(3), row(4), row(5), row(6), row(7))
  }.toMap
  
  r1.close()
  
  val r2 = new CSVReader(new FileReader(newCsvPath))
  
  val rows = r2.readAll().tail.map {
    row => Row(row(0), row(1), row(2).toBoolean, row(3), row(4), row(5), row(6), row(7), row(8))
  }
  
  r2.close()
  
  val updatedRows = rows.map {
    row =>
      translated.get((row.foodCode, row.assocCode)) match {
        case Some(t) => row.copy(localPrompt = t.localPrompt, localGenericName = t.localGenericName)
        case None => row
      }
  }
  
  val writer = new CSVWriter(new FileWriter(outputPath))
  
  writer.writeNext(Array("Food code", "Associated food or category", "Link as main", "English food description", "Portuguese food description", "English prompt text", "Portuguese prompt text", "English generic food name", "Portuguese generic food name"))
  
  updatedRows.foreach {
    row =>
      writer.writeNext(Array(row.foodCode, row.assocCode, row.linkAsMain.toString(), row.englishDesc, row.localDesc, row.englishPrompt, row.localPrompt, row.englishGenericName, row.localGenericName))
  }
  
  writer.close()

}