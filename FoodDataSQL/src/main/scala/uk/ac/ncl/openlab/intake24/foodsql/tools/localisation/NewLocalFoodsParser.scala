package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConverters._
import uk.ac.ncl.openlab.intake24.NewMainFoodRecord
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.NewLocalFoodRecord
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.AssociatedFood
import org.slf4j.LoggerFactory

trait NewLocalFoodsParser {
  
  private val logger = LoggerFactory.getLogger(classOf[NewLocalFoodsParser])
  
  def buildNewLocalFoods(csvPath: String, localeCode: String, localNutrientTableId: String, assocFoods: Map[String, Seq[AssociatedFood]]) = {
    val reader = new CSVReader(new FileReader(csvPath))
    
    var rows = reader.readAll().asScala.toSeq

    val newFoodRecords = rows.map {
      row =>
        val parentCategories = row.drop(5).filterNot(_.isEmpty()).toSeq

        NewMainFoodRecord(row(0), row(2), 0, InheritableAttributes(None, None, None), parentCategories, Seq(localeCode))
    }

    val newLocalRecords = rows.map {
      row =>
        
        val code = row(0)
        
        val associatedFoods = assocFoods.getOrElse(code, Seq())
        val portionSizeMethods = Seq() // Cannot be set right now due to circular dependencies
        
        code -> NewLocalFoodRecord(Some(row(3)), false, Map(localNutrientTableId -> row(4)), portionSizeMethods, associatedFoods, Seq())
    }.toMap

    reader.close()

    (newFoodRecords, newLocalRecords)
  }
}