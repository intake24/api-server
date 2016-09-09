package uk.ac.ncl.openlab.intake24.foodsql

import java.sql.Connection

import scala.Left

import org.postgresql.util.PSQLException

import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import anorm.ResultSetParser
import anorm.SqlQueryResult
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import anorm.NamedParameter
import anorm.BatchSql

trait SqlDataService {
  val dataSource: DataSource

  /**
   * Anorm's BatchSql with Seq[Seq[NamedParameter]] is deprecated due to the requirement
   * that there must be at least one set of parameters. The new apply method signature
   * essentially accepts a non-empty list, but it is difficult to pass the result of a map
   * to it since Scala has no built-in empty list type.
   *
   * This method converts an old style Seq[Seq[NamedParameter]] to the non-empty-list format
   * required by the new apply method. The non-emptyness restriction is not enforced and will
   * result in a "head of an empty list" error.
   */
  def batchSql(query: String, parameters: Seq[Seq[NamedParameter]]) = {
    BatchSql(query, parameters.head, parameters.tail: _*)
  }

  def withTransaction[E, T](block: => Either[E, T])(implicit conn: java.sql.Connection): Either[E, T] = {
    conn.setAutoCommit(false)
    conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)

    block match {
      case result: Left[E, T] => {
        conn.rollback()
        result
      }
      case result: Right[E, T] => {
        conn.commit()
        result
      }
    }
  }

  def tryWithConstraintsCheck[E >: DatabaseError, T](cf: PartialFunction[String, PSQLException => E])(block: => Either[E, T])(implicit conn: java.sql.Connection): Either[E, T] = {
    def getErrorOrRethrow(e: PSQLException): Either[E, T] = {
      val constraint = e.getServerErrorMessage.getConstraint

      if (cf.isDefinedAt(constraint)) {
        Left(cf(constraint)(e))
      } else
        throw e
    }

    try {
      block
    } catch {
      case e: java.sql.BatchUpdateException => e.getNextException match {
        case pe: PSQLException => getErrorOrRethrow(pe)
        case _ => throw e
      }
      case pe: PSQLException => getErrorOrRethrow(pe)
    }
  }

  def tryWithConstraintCheck[E >: DatabaseError, T](constraint: String, error: PSQLException => E)(block: => Either[E, T])(implicit conn: java.sql.Connection): Either[E, T] = {
    try {
      block
    } catch {
      case e: java.sql.BatchUpdateException => e.getNextException match {
        case pe: PSQLException => if (pe.getServerErrorMessage.getConstraint == constraint) {
          if (!conn.getAutoCommit())
            conn.rollback()
          Left(error(pe))
        } else throw e
        case _ => throw e
      }
      case pe: PSQLException => if (pe.getServerErrorMessage.getConstraint == constraint) {
        if (!conn.getAutoCommit())
          conn.rollback()
        Left(error(pe))
      } else throw pe
    }
  }

  def tryWithConnection[E >: DatabaseError, T](block: Connection => Either[E, T]): Either[E, T] = {
    val conn = dataSource.getConnection()
    try {
      block(conn)
    } catch {
      case e: Throwable => {
        if (!conn.getAutoCommit())
          conn.rollback()
        e match {
          case batchException: java.sql.BatchUpdateException => Left(DatabaseError(batchException.getNextException))
          case sqlException: PSQLException => Left(DatabaseError(sqlException))
          case _ => throw e
        }
      }
    } finally {
      conn.close()
    }
  }
}
