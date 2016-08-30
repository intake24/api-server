package controllers

import uk.ac.ncl.openlab.intake24.services.fooddb.errors._

import play.api.http.ContentTypes
import play.api.mvc.Results

import upickle.default._

trait ApiErrorHandler extends Results {

  def translateResult[T](result: Either[DatabaseError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(message).as(ContentTypes.JSON)
  }

  def translateResult[T](result: Either[LookupError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(RecordNotFound) => NotFound.as(ContentTypes.JSON)
  }

  def translateResult[T](result: Either[LocaleError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(UndefinedLocale) => NotFound.as(ContentTypes.JSON)
  }

  def translateResult[T](result: Either[LocalLookupError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(UndefinedLocale) => NotFound.as(ContentTypes.JSON)
  }

  def translateResult[T](result: Either[UpdateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(message)
  }

  def translateResult[T](result: Either[LocalUpdateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(message)
  }

  def translateResult[T](result: Either[DependentCreateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(message)
  }

  def translateResult[T](result: Either[DeleteError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(message)
  }

  def translateResult[T](result: Either[CreateError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseError(message, _)) => InternalServerError(message)
  }
}