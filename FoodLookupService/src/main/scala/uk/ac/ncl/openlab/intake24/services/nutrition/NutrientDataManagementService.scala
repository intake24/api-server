package uk.ac.ncl.openlab.intake24.services.nutrition

import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.NutrientType
import uk.ac.ncl.openlab.intake24.NutrientTableRecord

trait NutrientDataManagementService {
  def nutrientTables(): Seq[NutrientTable]
  def nutrientTable(id: String): Option[NutrientTable]
  def createNutrientTable(data: NutrientTable)
  def updateNutrientTable(id: String, data: NutrientTable)
  def deleteNutrientTable(id: String)
  
  //def nutrientTypes(): Seq[NutrientType]
  //def nutrientType(id: Int): Option[NutrientType]
  def createNutrientType(record: NutrientType)
  //def updateNutrientType(id: Int, record: NutrientType)
  //def deleteNutrientType(id: Int)
    
  def createNutrientTableRecords(records: Seq[NutrientTableRecord])
}
