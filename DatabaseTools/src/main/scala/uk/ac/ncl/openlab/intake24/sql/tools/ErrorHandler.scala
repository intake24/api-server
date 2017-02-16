package uk.ac.ncl.openlab.intake24.sql.tools

import uk.ac.ncl.openlab.intake24.errors.AnyError

trait ErrorHandler {

  def throwOnError[T](result: Either[AnyError, T]): T = result match {
    case Left(e) => throw e.exception
    case Right(r) => r
  }
}