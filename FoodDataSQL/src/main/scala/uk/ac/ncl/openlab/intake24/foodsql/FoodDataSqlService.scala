package uk.ac.ncl.openlab.intake24.foodsql

import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait FoodDataSqlService extends SqlDataService[DatabaseError] {
  override def defaultDatabaseError(e: PSQLException) = DatabaseError(e) 
}