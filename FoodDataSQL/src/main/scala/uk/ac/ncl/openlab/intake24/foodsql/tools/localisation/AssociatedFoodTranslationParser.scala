package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConverters._
import uk.ac.ncl.openlab.intake24.AssociatedFood

class AssociatedFoodTranslationParser {

  def parseAssociatedFoodTranslation(csvPath: String) = {
    val reader = new CSVReader(new FileReader(csvPath))

    val result = reader.readAll().asScala.tail.foldLeft(Map[String, Seq[AssociatedFood]]()) {
      (m, row) =>
        val foodCode = row(0)
        val assocCode = row(1)
        val linkAsMain = row(2).toBoolean
        val text = row(6)
        val genericName = row(7)

        text match {
          case "N/A" => m
          case "" => m
          case _ => m + (foodCode -> (m.get(foodCode).getOrElse(Seq()) :+ AssociatedFood(assocCode, text, linkAsMain, genericName)))
        }
    }

    reader.close()
    result
  }
}