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

  def translateDatabaseError[T](result: Either[DatabaseError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateLookupError[T](result: Either[LookupError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateLocaleError[T](result: Either[LocaleError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(UndefinedLocale(_)) => BadRequest(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateLocalLookupError[T](result: Either[LocalLookupError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(UndefinedLocale(_)) => BadRequest(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateUpdateError[T](result: Either[UpdateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DuplicateCode(_)) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(VersionConflict) => Conflict.as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateParentError[T](result: Either[ParentError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(ParentRecordNotFound(_)) => BadRequest(parentRecordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(IllegalParent(_)) => BadRequest(illegalParentErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateLocalUpdateError[T](result: Either[LocalUpdateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(UndefinedLocale(_)) => NotFound(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case Left(DuplicateCode(_)) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(VersionConflict) => Conflict.as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateDependentCreateError[T](result: Either[DependentCreateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DuplicateCode(_)) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(ParentRecordNotFound(_)) => BadRequest(parentRecordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(IllegalParent(_)) => BadRequest(illegalParentErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateDependentUpdateError[T](result: Either[DependentUpdateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DuplicateCode(_)) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(VersionConflict) => Conflict.as(ContentTypes.JSON)
    case Left(ParentRecordNotFound(_)) => BadRequest(parentRecordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(IllegalParent(_)) => BadRequest(illegalParentErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateLocalDependentUpdateError[T](result: Either[LocalDependentUpdateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DuplicateCode(_)) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(VersionConflict) => Conflict.as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(UndefinedLocale(_)) => BadRequest(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case Left(ParentRecordNotFound(_)) => BadRequest(parentRecordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(IllegalParent(_)) => BadRequest(illegalParentErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateDeleteError[T](result: Either[DeleteError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateCreateError[T](result: Either[CreateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DuplicateCode(_)) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }

  def translateLocaleOrParentError[T](result: Either[LocaleOrParentError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(UndefinedLocale(_)) => NotFound(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case Left(ParentRecordNotFound(_)) => BadRequest(parentRecordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(IllegalParent(_)) => BadRequest(illegalParentErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(exception)) => handleDatabaseError(exception)
  }
}