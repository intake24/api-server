package uk.ac.ncl.openlab.intake24.foodsql

import java.sql.Connection

import scala.Left

import org.postgresql.util.PSQLException

import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait SqlDataService {
  val dataSource: DataSource
  
   def tryWithConnection[E >: DatabaseError, T](block: Connection => Either[E, T]): Either[E, T] = {
    val conn = dataSource.getConnection()
    try {
      block(conn)
    } catch {
      case batchException: java.sql.BatchUpdateException => Left(DatabaseError(batchException.getNextException.getMessage, batchException))
      case sqlException: PSQLException => Left(DatabaseError(sqlException.getServerErrorMessage.getMessage, sqlException))
    } finally {      
      // rollback will be called automatically if commit wasn't called
      conn.close()
    }
  } 
}
