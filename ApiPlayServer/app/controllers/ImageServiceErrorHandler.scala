package controllers

import play.api.Logger
import play.api.http.ContentTypes
import play.api.mvc.{Result, Results}
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.{RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.fooddb.images._
import upickle.default._

trait ImageServiceErrorHandler extends Results {

  private def genericErrorBody(e: Throwable) = s"""{"cause":"${e.getClass.getName}: ${e.getMessage}"}"""

  private def logException(e: Throwable) = Logger.error("Image service exception", e)

  def translateImageServiceError(error: ImageServiceError): Result = error match {
    case IOError(e) => {
      logException(e)
      InternalServerError(genericErrorBody(e))
    }
    case ImageProcessorError(e) => {
      logException(e)
      InternalServerError(genericErrorBody(e))
    }
    case ImageStorageError(e) => {
      logException(e)
      InternalServerError(genericErrorBody(e))
    }
    case FileTypeNotAllowed(e) => {
      logException(e)
      BadRequest(genericErrorBody(e))
    }
  }

  def translateImageServiceResult [T](result: Either[ImageServiceError, T])(implicit writer: Writer[T]): Result = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(error) => translateImageServiceError(error)
  }
}