package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConverters._

trait CategoryTranslationParser {
  def parseCategoryTranslations(path: String): Map[String, String] = {
    val reader = new CSVReader(new FileReader(path))
    val result = reader.readAll().asScala.tail.filterNot(_(2).isEmpty()).map(row => row(0) -> row(2)).toMap
    reader.close()
    result
  }
}
