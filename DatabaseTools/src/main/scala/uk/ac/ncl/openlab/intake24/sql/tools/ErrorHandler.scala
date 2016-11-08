package uk.ac.ncl.openlab.intake24.sql.tools

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.AnyDatabaseError

trait ErrorHandler {

  def throwOnError[T](result: Either[AnyDatabaseError, T]): T = result match {
    case Left(e) => throw e.exception
    case Right(r) => r
  }
}