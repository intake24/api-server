package uk.ac.ncl.openlab.intake24.services.util

import uk.ac.ncl.openlab.intake24.errors.DatabaseError

trait DbErrorHandler {

  def throwOnError[T](result: Either[DatabaseError, T]): T = result match {
    case Left(e) => throw e.exception
    case Right(r) => r
  }
}