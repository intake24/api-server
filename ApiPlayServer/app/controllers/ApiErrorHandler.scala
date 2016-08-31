package controllers

import uk.ac.ncl.openlab.intake24.services.fooddb.errors._

import play.api.http.ContentTypes
import play.api.mvc.Results

import upickle.default._

trait ApiErrorHandler extends Results {

  def databaseErrorBody(message: String) = s"""{"cause":"DatabaseError","errorMessage":"$message"}"""
  val recordNotFoundErrorBody = s"""{cause:"RecordNotFound"}"""
  val undefinedLocaleErrorBody = s"""{cause:"UndefinedLocale"}"""
  val duplicateCodeErrorBody = s"""{cause:"DuplicateCode"}"""
  val parentRecordNotFoundErrorBody = s"""{cause:"ParentRecordNotFound"}"""

  def translateDatabaseError[T](result: Either[DatabaseError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(databaseErrorBody(message)).as(ContentTypes.JSON)
  }

  def translateLookupError[T](result: Either[LookupError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(databaseErrorBody(message)).as(ContentTypes.JSON)
  }

  def translateLocaleError[T](result: Either[LocaleError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(UndefinedLocale) => NotFound(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(databaseErrorBody(message)).as(ContentTypes.JSON)
  }

  def translateLocalLookupError[T](result: Either[LocalLookupError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(UndefinedLocale) => NotFound(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(databaseErrorBody(message)).as(ContentTypes.JSON)
  }

  def translateUpdateError[T](result: Either[UpdateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DuplicateCode) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(VersionConflict) => Conflict.as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(databaseErrorBody(message)).as(ContentTypes.JSON)
  }

  def translateLocalUpdateError[T](result: Either[LocalUpdateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(UndefinedLocale) => NotFound(undefinedLocaleErrorBody).as(ContentTypes.JSON)
    case Left(DuplicateCode) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(VersionConflict) => Conflict.as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(databaseErrorBody(message)).as(ContentTypes.JSON)
  }

  def translateDependentCreateError[T](result: Either[DependentCreateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DuplicateCode) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(ParentRecordNotFound) => BadRequest(parentRecordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(message)
  }

  def translateDeleteError[T](result: Either[DeleteError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound(recordNotFoundErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(message)
  }

  def translateCreateError[T](result: Either[CreateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DuplicateCode) => BadRequest(duplicateCodeErrorBody).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(message)
  }
}