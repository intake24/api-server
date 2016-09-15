package controllers

import uk.ac.ncl.openlab.intake24.services.fooddb.errors._

import play.api.http.ContentTypes
import play.api.mvc.Results

import upickle.default._
import org.slf4j.LoggerFactory

trait ApiErrorHandler extends Results {

  private val logger = LoggerFactory.getLogger(classOf[ApiErrorHandler])

  def databaseErrorBody(message: String) = s"""{"cause":"DatabaseError","errorMessage":"$message"}"""
  val recordNotFoundErrorBody = s"""{"cause":"RecordNotFound}"""
  val undefinedLocaleErrorBody = s"""{"cause":"UndefinedLocale"}"""
  val duplicateCodeErrorBody = s"""{"cause":"DuplicateCode"}"""
  val parentRecordNotFoundErrorBody = s"""{"cause":"ParentRecordNotFound"}"""
  val illegalParentErrorBody = s"""{"cause":"IllegalParent"}"""

  def handleDatabaseError(e: Throwable) = {
    logger.error("DatabaseError", e)
    InternalServerError(databaseErrorBody(e.getMessage())).as(ContentTypes.JSON)
  }

  def translateError[T](result: Either[AnyError, T])(implicit writer: Writer[T]) = result match {
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