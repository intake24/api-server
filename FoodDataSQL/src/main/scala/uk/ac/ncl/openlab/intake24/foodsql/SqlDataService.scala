package uk.ac.ncl.openlab.intake24.foodsql

import java.sql.Connection

import scala.Left

import org.postgresql.util.PSQLException

import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import anorm.ResultSetParser
import anorm.SqlQueryResult
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalFoodCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedCode
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalCategoryCodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodCodeError

trait SqlDataService {
  val dataSource: DataSource

  def tryWithConnection[E >: DatabaseError, T](block: Connection => Either[E, T]): Either[E, T] = {
    val conn = dataSource.getConnection()
    try {
      block(conn)
    } catch {
      case batchException: java.sql.BatchUpdateException => Left(DatabaseError(batchException.getNextException.getMessage, Some(batchException)))
      case sqlException: PSQLException => Left(DatabaseError(sqlException.getServerErrorMessage.getMessage, Some(sqlException)))
    } finally {
      // rollback will be called automatically if commit wasn't called
      conn.close()
    }
  }
}
