package controllers

import play.api.http.ContentTypes
import play.api.mvc.Result
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{DatabaseErrorWrapper, ImageServiceErrorWrapper, ImageServiceOrDatabaseError}
import upickle.default._

trait ImageOrDatabaseServiceErrorHandler extends FoodDatabaseErrorHandler with ImageServiceErrorHandler {

  def translateImageServiceAndDatabaseError[T](result: Either[ImageServiceOrDatabaseError, T]): Either[Result, T] = result match {
    case Right(result) => Right(result)
    case Left(DatabaseErrorWrapper(error)) => Left(translateDatabaseError(error))
    case Left(ImageServiceErrorWrapper(error)) => Left(translateImageServiceError(error))
  }

  def translateImageServiceAndDatabaseResult[T](result: Either[ImageServiceOrDatabaseError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseErrorWrapper(error)) => translateDatabaseError(error)
    case Left(ImageServiceErrorWrapper(error)) => translateImageServiceError(error)
  }
}