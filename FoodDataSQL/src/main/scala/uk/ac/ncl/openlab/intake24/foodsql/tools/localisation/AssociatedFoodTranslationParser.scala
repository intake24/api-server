package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import java.io.FileReader

import scala.collection.JavaConverters.asScalaBufferConverter

import au.com.bytecode.opencsv.CSVReader
import uk.ac.ncl.openlab.intake24.AssociatedFoodV1

class AssociatedFoodTranslationParser {

  def parseAssociatedFoodTranslation(csvPath: String) = {
    val reader = new CSVReader(new FileReader(csvPath))

    val result = reader.readAll().asScala.tail.foldLeft(Map[String, Seq[AssociatedFoodV1]]()) {
      (m, row) =>
        val foodCode = row(0)
        val assocCategoryCode = row(1)
        val linkAsMain = row(2).toBoolean
        val text = row(6)
        val genericName = row(7)
        
        text match {
          case "N/A" => m
          case "" => m
          case _ => m + (foodCode -> (m.get(foodCode).getOrElse(Seq()) :+ AssociatedFoodV1(assocCategoryCode, text, linkAsMain, genericName)))
        }
    }

    reader.close()
    result
  }
}