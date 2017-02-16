package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.errors.{LookupError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.{NutrientTable, NutrientTableRecord}

trait NutrientTablesAdminService {

  def listNutrientTables(): Either[UnexpectedDatabaseError, Map[String, NutrientTable]]

  def getNutrientTable(id: String): Either[LookupError, NutrientTable]

  def createNutrientTable(data: NutrientTable): Either[UnexpectedDatabaseError, Unit]

  def updateNutrientTable(id: String, data: NutrientTable): Either[LookupError, Unit]

  def deleteNutrientTable(id: String): Either[LookupError, Unit]

  def deleteAllNutrientTables(): Either[UnexpectedDatabaseError, Unit]

  def createNutrientTableRecords(records: Seq[NutrientTableRecord]): Either[UnexpectedDatabaseError, Unit]
}

