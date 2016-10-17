package controllers

import scala.annotation.implicitNotFound

import org.slf4j.LoggerFactory

import play.api.http.ContentTypes
import play.api.mvc.Results

import upickle.default._
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageServiceError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.images.IOError
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageProcessorError
import uk.ac.ncl.openlab.intake24.services.fooddb.images.ImageStorageError

import play.api.Logger
import uk.ac.ncl.openlab.intake24.services.fooddb.images.FileTypeNotAllowed
import play.api.mvc.Result

trait ImageServiceErrorHandler extends Results {

  def genericErrorBody(e: Throwable) = s"""{"cause":"${e.getClass.getName}: ${e.getMessage}"}"""

  def logException(e: Throwable) = Logger.error("Image service exception", e)

  def translateError(error: ImageServiceError): Result = error match {
    case ImageDatabaseError(DatabaseError(e)) => {
      logException(e)
      InternalServerError(genericErrorBody(e))
    }
    case ImageDatabaseError(RecordNotFound(exception)) => {
      logException(exception)
      NotFound(genericErrorBody(exception)).as(ContentTypes.JSON)
    }
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

  def translateError[T](result: Either[ImageServiceError, T])(implicit writer: Writer[T]): Result = result match {
    case Right(result) => Ok(write(result)).as(ContentTypes.JSON)
    case Left(error) => translateError(error)
  }
}