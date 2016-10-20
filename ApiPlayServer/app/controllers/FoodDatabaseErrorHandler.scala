package controllers

import scala.annotation.implicitNotFound

import org.slf4j.LoggerFactory

import play.api.http.ContentTypes
import play.api.mvc.Results
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.IllegalParent
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.VersionConflict
import upickle.default.Writer
import upickle.default.write

trait FoodDatabaseErrorHandler extends Results {

  private val logger = LoggerFactory.getLogger(classOf[FoodDatabaseErrorHandler])

  def databaseErrorBody(message: String) = s"""{"cause":"DatabaseError","errorMessage":"$message"}"""
  val recordNotFoundErrorBody = s"""{"cause":"RecordNotFound"}"""
  val undefinedLocaleErrorBody = s"""{"cause":"UndefinedLocale"}"""
  val duplicateCodeErrorBody = s"""{"cause":"DuplicateCode"}"""
  val parentRecordNotFoundErrorBody = s"""{"cause":"ParentRecordNotFound"}"""
  val illegalParentErrorBody = s"""{"cause":"IllegalParent"}"""

  def handleDatabaseError(e: Throwable) = {
    logger.error("DatabaseError", e)
    InternalServerError(databaseErrorBody(e.getMessage())).as(ContentTypes.JSON)
  }

  def translateResult[T](result: Either[AnyError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DuplicateCode(_)) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(VersionConflict(_)) => Conflict.as(ContentTypes.JSON)
    case Left(RecordNotFound(_)) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(UndefinedLocale(_)) => BadRequest(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case Left(ParentRecordNotFound(_)) => BadRequest(parentRecordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(IllegalParent(_)) => BadRequest(illegalParentErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }
}