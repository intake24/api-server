package uk.ac.ncl.openlab.intake24.systemsql

import org.postgresql.util.PSQLException

import uk.ac.ncl.openlab.intake24.services.systemdb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

trait SystemSqlService extends SqlDataService[UnexpectedDatabaseError] {
  override def defaultDatabaseError(e: PSQLException) = UnexpectedDatabaseError(e)
}