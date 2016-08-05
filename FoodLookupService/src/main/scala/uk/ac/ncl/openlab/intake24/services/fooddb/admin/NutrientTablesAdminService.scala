package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait NutrientTablesAdminService {
  
  def nutrientTables(): Either[DatabaseError, Seq[NutrientTable]] 
}
