package controllers

import org.slf4j.LoggerFactory
import play.api.http.ContentTypes
import play.api.mvc.{Result, Results}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.errors._

import upickle.default._

trait DatabaseErrorHandler extends Results {

  private val logger = LoggerFactory.getLogger(classOf[DatabaseErrorHandler])

  private def handleDatabaseError(e: Throwable): Result = {

    def logCause(e: Throwable): Unit =
      if (e != null) {
        logger.error("Caused by", e)
        logCause(e.getCause)
      }

    logger.error("DatabaseError", e)
    logCause(e.getCause)

    InternalServerError(write(ErrorDescription("DatabaseError", "Unexpected database error: " + e.getMessage()))).as(ContentTypes.JSON)
  }

  def translateDatabaseError(error: AnyError): Result = error match {
    case DuplicateCode(_) => BadRequest(write(ErrorDescription("DuplicateCode", "Object with this code or id already exists, duplicate codes are not allowed"))).as(ContentTypes.JSON)
    case VersionConflict(_) => Conflict(write(ErrorDescription("VersionConflict", "Object has been concurrently edited by someone else, try again using the new base version"))).as(ContentTypes.JSON)
    case RecordNotFound(e) => NotFound(write(ErrorDescription("RecordNotFound", "Object does not exist: " + e.getMessage))).as(ContentTypes.JSON)
    case StillReferenced(_) => BadRequest(write(ErrorDescription("StillReferenced", "Object cannot be deleted because it is still referenced by other objects, delete them first"))).as(ContentTypes.JSON)
    case UndefinedLocale(_) => BadRequest(write(ErrorDescription("UndefinedLocale", "Locale is not defined, check the locale code"))).as(ContentTypes.JSON)
    case ParentRecordNotFound(e) => BadRequest(write(ErrorDescription("ParentRecordNotFound", "An object referenced by this object does not exist: " + e.getMessage))).as(ContentTypes.JSON)
    case IllegalParent(_) => BadRequest(write(ErrorDescription("IllegalParent", "The object references an illegal parent object"))).as(ContentTypes.JSON)
    case ConstraintViolation(name, _) => BadRequest(write(ErrorDescription("ConstraintViolation", s"Database constraint not met: $name")))
    case UnexpectedDatabaseError(exception) => handleDatabaseError(exception)
  }

  def translateDatabaseResult[T](result: Either[AnyError, T])(implicit writer: Writer[T]) = result match {
    case Right(()) => Ok
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(error) => translateDatabaseError(error)
  }
}