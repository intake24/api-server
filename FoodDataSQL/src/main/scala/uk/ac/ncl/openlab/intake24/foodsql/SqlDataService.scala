package uk.ac.ncl.openlab.intake24.foodsql

import java.sql.Connection

import scala.Left

import org.postgresql.util.PSQLException

import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import anorm.ResultSetParser
import anorm.SqlQueryResult

trait SqlDataService {
  val dataSource: DataSource

  def parseWithFirstRowValidation[T, E](result: SqlQueryResult, errorTypes: Map[String, E], parser: ResultSetParser[T]): Either[E, T] = {
    result.withResult {
      cursorOpt =>
        val firstRow = cursorOpt.get.row
        val columns = firstRow.asMap
        
        errorTypes.keySet.map {
          columnName => 
            
        }
        
    }
  }

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
