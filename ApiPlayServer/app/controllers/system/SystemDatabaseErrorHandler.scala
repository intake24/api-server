package controllers.system

import org.slf4j.LoggerFactory
import play.api.http.ContentTypes
import play.api.mvc.{Result, Results}
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.systemdb.errors._
import upickle.default._

trait SystemDatabaseErrorHandler extends Results {

  private val logger = LoggerFactory.getLogger(classOf[SystemDatabaseErrorHandler])

  private def handleDatabaseError(e: Throwable): Result = {
    logger.error("DatabaseError", e)
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
    case UnexpectedDatabaseError(exception) => handleDatabaseError(exception)
  }

  def translateDatabaseResult[T](result: Either[AnyError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(error) => translateDatabaseError(error)
  }
}