package uk.ac.ncl.openlab.intake24.sql.tools

import uk.ac.ncl.openlab.intake24.errors.DatabaseError

trait ErrorHandler {

  def throwOnError[T](result: Either[DatabaseError, T]): T = result match {
    case Left(e) => throw e.exception
    case Right(r) => r
  }
}