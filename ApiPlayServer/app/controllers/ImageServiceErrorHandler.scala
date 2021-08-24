package controllers

import io.circe.Encoder
import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import parsers.JsonUtils
import play.api.http.ContentTypes
import play.api.mvc.{Result, Results}
import uk.ac.ncl.openlab.intake24.api.data.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.fooddb.images._

trait ImageServiceErrorHandler extends Results with JsonUtils {

  private val logger = LoggerFactory.getLogger(classOf[ImageServiceErrorHandler])

  private def genericErrorBody(e: Throwable) = toJsonString(ErrorDescription(e.getClass.getSimpleName, e.getMessage))

  private def logException(e: Throwable) = logger.error("Image service exception", e)

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

  def translateImageServiceResult[T](result: Either[ImageServiceError, T])(implicit enc: Encoder[T]): Result = result match {
    case Right(result) => Ok(toJsonString(result)).as(ContentTypes.JSON)
    case Left(error) => translateImageServiceError(error)
  }
}
