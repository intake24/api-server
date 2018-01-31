package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import java.io.FileReader

import au.com.bytecode.opencsv.CSVReader
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.data.admin.{NewLocalFoodRecord, NewMainFoodRecord}
import uk.ac.ncl.openlab.intake24.api.data.{AssociatedFood, InheritableAttributes}

import scala.collection.JavaConverters._

trait NewLocalFoodsParser {

  private val logger = LoggerFactory.getLogger(classOf[NewLocalFoodsParser])

  def buildNewLocalFoods(csvPath: String, localeCode: String, localNutrientTableId: String, assocFoods: Map[String, Seq[AssociatedFood]]) = {
    val reader = new CSVReader(new FileReader(csvPath))

    var rows = reader.readAll().asScala.toSeq

    val newFoodRecords = rows.map {
      row =>
        val parentCategories = row.drop(5).filterNot(_.isEmpty()).toSeq

        NewMainFoodRecord(row(0), row(2).replace("\"", ""), 0, InheritableAttributes(None, None, None), parentCategories, Seq(localeCode))
    }

    val newLocalRecords = rows.map {
      row =>

        val code = row(0)

        val associatedFoods = assocFoods.getOrElse(code, Seq())
        val portionSizeMethods = Seq() // Cannot be set right now due to circular dependencies

        code -> NewLocalFoodRecord(Some(row(3).replace("\"", "")), false, Map(localNutrientTableId -> row(4)), portionSizeMethods, associatedFoods, Seq())
    }.toMap

    reader.close()

    (newFoodRecords, newLocalRecords)
  }
}