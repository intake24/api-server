package uk.ac.ncl.openlab.intake24.foodsql.tools

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.VersionConflict
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.IllegalParent
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait ErrorHandler {

  def throwOnError[T](result: Either[AnyError, T]): T = result match {
    case Left(e) => throw e.exception
    case Right(r) => r
  }
}