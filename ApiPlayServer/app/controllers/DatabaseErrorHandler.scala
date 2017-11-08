package controllers

import io.circe.Encoder
import org.slf4j.LoggerFactory
import play.api.http.ContentTypes
import play.api.mvc.{Result, Results}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.errors._
import io.circe.generic.auto._
import parsers.JsonUtils

trait DatabaseErrorHandler extends Results with JsonUtils {

  private val logger = LoggerFactory.getLogger(classOf[DatabaseErrorHandler])

  private def handleDatabaseError(e: Throwable): Result = {

    def logCause(e: Throwable): Unit =
      if (e != null) {
        logger.error("Caused by", e)
        logCause(e.getCause)
      }

    logger.error("DatabaseError", e)
    logCause(e.getCause)

    InternalServerError(toJsonString(ErrorDescription("DatabaseError", "Unexpected database error: " + e.getMessage()))).as(ContentTypes.JSON)
  }

  def translateDatabaseError(error: AnyError): Result = error match {
    case DuplicateCode(e) => BadRequest(toJsonString(ErrorDescription("DuplicateCode", e.getMessage))).as(ContentTypes.JSON)
    case VersionConflict(_) => Conflict(toJsonString(ErrorDescription("VersionConflict", "Object has been concurrently edited by someone else, try again using the new base version"))).as(ContentTypes.JSON)
    case TableNotFound(e) => NotFound(toJsonString(ErrorDescription("RecordNotFound", "Food composition table not found: " + e.getMessage))).as(ContentTypes.JSON)
    case RecordNotFound(e) => NotFound(toJsonString(ErrorDescription("RecordNotFound", "Object does not exist: " + e.getMessage))).as(ContentTypes.JSON)
    case StillReferenced(e) => BadRequest(toJsonString(ErrorDescription("StillReferenced", e.getMessage))).as(ContentTypes.JSON)
    case UndefinedLocale(_) => BadRequest(toJsonString(ErrorDescription("UndefinedLocale", "Locale is not defined, check the locale code"))).as(ContentTypes.JSON)
    case ParentRecordNotFound(e) => BadRequest(toJsonString(ErrorDescription("ParentRecordNotFound", "An object referenced by this object does not exist: " + e.getMessage))).as(ContentTypes.JSON)
    case IllegalParent(_) => BadRequest(toJsonString(ErrorDescription("IllegalParent", "The object references an illegal parent object"))).as(ContentTypes.JSON)
    case ConstraintViolation(name, _) => BadRequest(toJsonString(ErrorDescription("ConstraintViolation", s"Database constraint not met: $name")))
    case FailedValidation(exception) => handleDatabaseError(exception)
    case UnexpectedDatabaseError(exception) => handleDatabaseError(exception)
  }

  def translateDatabaseResult[T](result: Either[AnyError, T])(implicit enc: Encoder[T]) = result match {
    case Right(()) => Ok
    case Right(result) => Ok(toJsonString(result)).as(ContentTypes.JSON)
    case Left(error) => translateDatabaseError(error)
  }
}
