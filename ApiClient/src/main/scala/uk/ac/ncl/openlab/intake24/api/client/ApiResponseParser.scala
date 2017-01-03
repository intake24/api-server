package uk.ac.ncl.openlab.intake24.api.client

import upickle.default._

import scalaj.http.HttpResponse

trait ApiResponseParser {

  private case class ErrorDescription(cause: String, errorMessage: String)

  def parseApiResponse[T](response: HttpResponse[String])(implicit reader: Reader[T]): Either[ApiError, T] = {
    if (response.code == 200) {
      try {
        Right(read[T](response.body))
      } catch {
        case e: Throwable => Left(ApiError.ResultParseFailed(e))
      }
    } else {
      try {
        val desc = read[ErrorDescription](response.body)
        Left(ApiError.RequestFailed(response.code, desc.cause, desc.errorMessage))
      } catch {
        case e: Throwable => Left(ApiError.ErrorParseFailed(response.code, e))
      }
    }
  }

}
