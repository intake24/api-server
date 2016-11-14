package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.NutrientTableRecord

trait NutrientTablesAdminService {

  def listNutrientTables(): Either[UnexpectedDatabaseError, Map[String, NutrientTable]]
  def getNutrientTable(id: String): Either[LookupError, NutrientTable]
  def createNutrientTable(data: NutrientTable): Either[UnexpectedDatabaseError, Unit]
  def updateNutrientTable(id: String, data: NutrientTable): Either[LookupError, Unit]
  def deleteNutrientTable(id: String): Either[LookupError, Unit]
  def deleteAllNutrientTables(): Either[UnexpectedDatabaseError, Unit]

  def createNutrientTableRecords(records: Seq[NutrientTableRecord]): Either[UnexpectedDatabaseError, Unit]
}
