package uk.ac.ncl.openlab.intake24.services.nutrition

import uk.ac.ncl.openlab.intake24.errors.{LookupError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.{NutrientTable, FoodCompositionRecord}

trait FoodCompositionTableAdminService {
  def listTables(): Either[UnexpectedDatabaseError, Seq[NutrientTable]]

  def getTable(id: String): Either[LookupError, NutrientTable]

  def createTable(data: NutrientTable): Either[UnexpectedDatabaseError, Unit]

  def updateTable(id: String, data: NutrientTable): Either[LookupError, Unit]

  def deleteTable(id: String): Either[LookupError, Unit]

  //def nutrientTypes(): Seq[NutrientType]
  //def nutrientType(id: Int): Option[NutrientType]
  //def createNutrientTypes(record: Seq[NutrientType])
  //def updateNutrientType(id: Int, record: NutrientType)
  //def deleteNutrientType(id: Int)

  def createFoodCompositionRecords(records: Seq[FoodCompositionRecord]): Either[UnexpectedDatabaseError, Unit]
}
