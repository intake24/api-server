package controllers

import org.slf4j.LoggerFactory
import play.api.Logger
import play.api.http.ContentTypes
import play.api.mvc.{Result, Results}
import uk.ac.ncl.openlab.intake24.services.fooddb.errors._
import upickle.default._

trait FoodDatabaseErrorHandler extends Results {

  private val logger = LoggerFactory.getLogger(classOf[FoodDatabaseErrorHandler])

  private def databaseErrorBody(message: String) = s"""{"cause":"DatabaseError","errorMessage":"$message"}"""

  private val recordNotFoundErrorBody = s"""{"cause":"RecordNotFound"}"""
  private val undefinedLocaleErrorBody = s"""{"cause":"UndefinedLocale"}"""
  private val duplicateCodeErrorBody = s"""{"cause":"DuplicateCode"}"""
  private val parentRecordNotFoundErrorBody = s"""{"cause":"ParentRecordNotFound"}"""
  private val illegalParentErrorBody = s"""{"cause":"IllegalParent"}"""
  private val stillReferencedErrorBody = s"""{"cause": "StillReferenced"}"""


  private def logException(e: Throwable) = Logger.error("Database exception", e)

  private def handleDatabaseError(e: Throwable): Result = {
    logger.error("DatabaseError", e)
    InternalServerError(databaseErrorBody(e.getMessage())).as(ContentTypes.JSON)
  }

  def translateDatabaseError(error: AnyError): Result = error match {
    case DuplicateCode(_) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case VersionConflict(_) => Conflict.as(ContentTypes.JSON)
    case RecordNotFound(_) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case StillReferenced(_) => BadRequest(stillReferencedErrorBody).as(ContentTypes.JSON)
    case UndefinedLocale(_) => BadRequest(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case ParentRecordNotFound(_) => BadRequest(parentRecordNotFoundErrorBody).as(ContentTypes.JSON)
    case IllegalParent(_) => BadRequest(illegalParentErrorBody).as(ContentTypes.JSON)
    case UnexpectedDatabaseError(exception) => handleDatabaseError(exception)
  }

  def translateDatabaseResult[T](result: Either[AnyError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(error) => translateDatabaseError(error)
  }
}