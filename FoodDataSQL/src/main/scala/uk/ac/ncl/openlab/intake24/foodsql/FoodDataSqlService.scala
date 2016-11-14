package uk.ac.ncl.openlab.intake24.foodsql

import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError

trait FoodDataSqlService extends SqlDataService[UnexpectedDatabaseError] {
  override def defaultDatabaseError(e: PSQLException) = UnexpectedDatabaseError(e)
}