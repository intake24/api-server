package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.errors.{LookupError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.{NewNutrientTableRecord, NutrientTable, NutrientTableRecord}

case class SingleNutrientTypeUpdate(nutrientTableRecordId: String, newValue: Option[Double])

trait NutrientTablesAdminService {

  def listNutrientTables(): Either[UnexpectedDatabaseError, Map[String, NutrientTable]]

  def searchNutrientTableRecords(nutrientTableId: String, query: Option[String]): Either[UnexpectedDatabaseError, Seq[NutrientTableRecord]]

  def getNutrientTable(id: String): Either[LookupError, NutrientTable]

  def createNutrientTable(data: NutrientTable): Either[UnexpectedDatabaseError, Unit]

  def updateNutrientTable(id: String, data: NutrientTable): Either[LookupError, Unit]

  def createOrUpdateNutrientTable(data: NutrientTable): Either[UnexpectedDatabaseError, Unit]

  def deleteNutrientTable(id: String): Either[LookupError, Unit]

  def deleteAllNutrientTables(): Either[UnexpectedDatabaseError, Unit]

  def createNutrientTableRecords(records: Seq[NewNutrientTableRecord]): Either[UnexpectedDatabaseError, Unit]

  def createOrUpdateNutrientTableRecords(records: Seq[NewNutrientTableRecord]): Either[UnexpectedDatabaseError, Unit]

  def updateSingleNutrientType(nutrientTableId: String, nutrientTypeId: Long, updates: Seq[SingleNutrientTypeUpdate]): Either[UnexpectedDatabaseError, Unit]

  def getNutrientTableRecordIds(nutrientTableId: String): Either[UnexpectedDatabaseError, Seq[String]]

}

