package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import uk.ac.ncl.openlab.intake24.AssociatedFood
import java.io.OutputStreamWriter
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import java.io.FileWriter
import au.com.bytecode.opencsv.CSVWriter
import uk.ac.ncl.openlab.intake24.NewLocalFoodRecord
import java.io.File
import uk.ac.ncl.openlab.intake24.UserFoodHeader

trait RecodingTableParser {
  def parseRecodingTable(path: String): RecodingTable
}

trait RecodingTableUtil {
  def buildRecodedLocalFoodRecords(logPath: Option[String], englishLocaleName: String, localNutrientTableId: String,
    indexableFoods: Seq[UserFoodHeader], recodingTable: RecodingTable, translatedAssociatedFoods: Map[String, Seq[AssociatedFood]]) = {

    val logWriter = new CSVWriter(logPath.map(logPath => new FileWriter(new File(logPath))).getOrElse(new OutputStreamWriter(System.out)))

    logWriter.writeNext(Array("Intake24 code", "English food description", "Coding decision", s"$englishLocaleName description"))

    val records = indexableFoods.foldLeft(Map[String, NewLocalFoodRecord]()) {
      (result, header) =>

        val logHeaderCols = Array(header.code, header.localDescription)

        val associatedFoods = translatedAssociatedFoods.getOrElse(header.code, Seq())

        val portionSizeMethods = Seq() // Cannot be set here due to circular dependencies

        recodingTable.existingFoodsCoding.get(header.code) match {
          case Some(DoNotUse) => {
            logWriter.writeNext(logHeaderCols ++ Array(s"Not using in $englishLocaleName locale"))
            result + (header.code -> NewLocalFoodRecord(None, true, Map(), Seq(), Seq(), Seq()))
          }
          case Some(UseUKFoodTable(localDescription)) => {
            logWriter.writeNext(logHeaderCols ++ Array("Inheriting UK food composition table code", localDescription))
            result + (header.code -> NewLocalFoodRecord(Some(localDescription), false, Map(), portionSizeMethods, associatedFoods, Seq()))
          }
          case Some(UseLocalFoodTable(localDescription, localTableRecordId)) => {
            logWriter.writeNext(logHeaderCols ++ Array(s"Using $localNutrientTableId food composition table code", localDescription, localTableRecordId))
            result + (header.code -> NewLocalFoodRecord(Some(localDescription), false, Map(localNutrientTableId -> localTableRecordId), portionSizeMethods, associatedFoods, Seq()))
          }
          case None =>
            logWriter.writeNext(logHeaderCols ++ Array(s"Not in $englishLocaleName recoding table!"))
            result
        }
    }

    logWriter.close()

    records
  }
}