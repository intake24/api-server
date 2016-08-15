package uk.ac.ncl.openlab.intake24.services.nutrition

import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.NutrientType
import uk.ac.ncl.openlab.intake24.NutrientTableRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError

trait NutrientDataManagementService {
  def allNutrientTables(): Either[DatabaseError, Seq[NutrientTable]]
  def nutrientTable(id: String): Either[ResourceError, NutrientTable]
  def createNutrientTable(data: NutrientTable): Either[DatabaseError, Unit]
  def updateNutrientTable(id: String, data: NutrientTable): Either[ResourceError, Unit]
  def deleteNutrientTable(id: String): Either[ResourceError, Unit]
  
  //def nutrientTypes(): Seq[NutrientType]
  //def nutrientType(id: Int): Option[NutrientType]
  //def createNutrientTypes(record: Seq[NutrientType])
  //def updateNutrientType(id: Int, record: NutrientType)
  //def deleteNutrientType(id: Int)
    
  def createNutrientTableRecords(records: Seq[NutrientTableRecord]): Either[DatabaseError, Unit]
}
