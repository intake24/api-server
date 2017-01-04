package uk.ac.ncl.openlab.intake24.api.client

import upickle.default._

import scalaj.http.HttpResponse

trait ApiResponseParser {

  private case class ErrorDescription(cause: String, errorMessage: String)

  private def getResponseBody(response: HttpResponse[String]): Either[ApiError, String] = {
    if (response.code == 200) {
      Right(response.body)
    } else {
      try {
        val desc = read[ErrorDescription](response.body)
        Left(ApiError.RequestFailed(response.code, desc.cause, desc.errorMessage))
      } catch {
        case e: Throwable => Left(ApiError.ErrorParseFailed(response.code, e))
      }
    }
  }

  def parseApiResponseDiscardBody(response: HttpResponse[String]): Either[ApiError, Unit] = getResponseBody(response).right.map(_ => ())

  def parseApiResponse[T](response: HttpResponse[String])(implicit reader: Reader[T]): Either[ApiError, T] =
    getResponseBody(response) match {
      case Right(body) =>
        try {
          Right(read[T](body))
        } catch {
          case e: Throwable => Left(ApiError.ResultParseFailed(e))
        }
      case Left(error) => Left(error)
    }

}
