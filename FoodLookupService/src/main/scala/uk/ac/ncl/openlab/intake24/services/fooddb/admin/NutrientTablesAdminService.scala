package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.NutrientTableRecord

trait NutrientTablesAdminService {

  def listNutrientTables(): Either[DatabaseError, Map[String, NutrientTable]]
  def getNutrientTable(id: String): Either[LookupError, NutrientTable]
  def createNutrientTable(data: NutrientTable): Either[DatabaseError, Unit]
  def updateNutrientTable(id: String, data: NutrientTable): Either[LookupError, Unit]
  def deleteNutrientTable(id: String): Either[LookupError, Unit]
  def deleteAllNutrientTables(): Either[DatabaseError, Unit]

  def createNutrientTableRecords(records: Seq[NutrientTableRecord]): Either[DatabaseError, Unit]
}
