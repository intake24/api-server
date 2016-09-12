package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConverters._
import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.NewLocalFoodRecord

trait NewLocalFoodsParser {

  def parseNewLocalFoods(csvPath: String) = {
    val reader = new CSVReader(new FileReader(csvPath))
    
    var rows = reader.readAll().asScala.toSeq

    val newFoodRecords = rows.map {
      row =>
        val parentCategories = row.drop(5).filterNot(_.isEmpty()).toSeq

        NewFood(row(0), row(2), 0, InheritableAttributes(None, None, None), parentCategories)
    }

    val newLocalRecords = rows.map {
      row =>
        row(0) -> NewLocalFoodRecord(Some(row(3)), false, Map("PT_INSA" -> row(4)), Seq(), Seq(), Seq())
    }.toMap

    reader.close()

    (newFoodRecords, newLocalRecords)
  }
}