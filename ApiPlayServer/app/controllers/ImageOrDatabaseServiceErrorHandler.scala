package controllers

import play.api.http.ContentTypes
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{DatabaseErrorWrapper, ImageServiceErrorWrapper, ImageServiceOrDatabaseError}
import upickle.default._

trait ImageOrDatabaseServiceErrorHandler extends FoodDatabaseErrorHandler with ImageServiceErrorHandler {
  def translateImageServiceAndDatabaseResult[T](result: Either[ImageServiceOrDatabaseError, T])(implicit writer: Writer[T]) = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(DatabaseErrorWrapper(error)) => translateDatabaseError(error)
    case Left(ImageServiceErrorWrapper(error)) => translateImageServiceError(error)
  }
}