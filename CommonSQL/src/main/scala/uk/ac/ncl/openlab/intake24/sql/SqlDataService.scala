package uk.ac.ncl.openlab.intake24.sql

import java.sql.Connection
import javax.sql.DataSource

import anorm.{AnormException, BatchSql, NamedParameter}
import org.postgresql.util.PSQLException
import uk.ac.ncl.openlab.intake24.errors.{DatabaseError, UnexpectedDatabaseError}

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

    val result = block

    if (result.isLeft)
      conn.rollback()
    else
      conn.commit()

    result
  }

  def tryWithConstraintsCheck[E, T](cf: PartialFunction[String, PSQLException => E])(block: => Either[E, T])(implicit conn: java.sql.Connection): Either[E, T] = {
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

  def tryWithConstraintCheck[E, T](constraint: String, error: PSQLException => E)(block: => Either[E, T])(implicit conn: java.sql.Connection): Either[E, T] = {
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

  def tryWithConnection[E >: UnexpectedDatabaseError, T](block: Connection => Either[E, T]): Either[E, T] = {
    val conn = dataSource.getConnection()
    try {
      block(conn)
    } catch {
      case e: Throwable => {
        if (!conn.getAutoCommit())
          conn.rollback()
        e match {
          case batchException: java.sql.BatchUpdateException => Left(UnexpectedDatabaseError(batchException.getNextException.asInstanceOf[PSQLException]))
          case sqlException: PSQLException => Left(UnexpectedDatabaseError(sqlException))
          case anormException: AnormException => Left(UnexpectedDatabaseError(anormException))
          case _ => throw e
        }
      }
    } finally {
      conn.close()
    }
  }

  def tryWithConnectionWrapErrors[E, T](wrap: DatabaseError => E)(block: Connection => Either[E, T]): Either[E, T] = {
    val conn = dataSource.getConnection()
    try {
      block(conn)
    } catch {
      case e: Throwable => {
        if (!conn.getAutoCommit())
          conn.rollback()
        e match {
          case batchException: java.sql.BatchUpdateException => Left(wrap(UnexpectedDatabaseError(batchException.getNextException.asInstanceOf[PSQLException])))
          case sqlException: PSQLException => Left(wrap(UnexpectedDatabaseError(sqlException)))
          case _ => throw e
        }
      }
    } finally {
      conn.close()
    }
  }
}
