package uk.ac.ncl.openlab.intake24.api.client

import io.circe.Decoder
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription

import scalaj.http.HttpResponse
import io.circe.generic.auto._

trait ApiResponseParser extends JsonParser {

  private def getResponseBody(response: HttpResponse[String]): Either[ApiError, String] = response.code match {
    case 200 => Right(response.body)
    case 401 => Left(ApiError.RequestFailed(401, "Forbidden", "Forbidden"))
    case 403 => Left(ApiError.RequestFailed(403, "Unauthorized", "Unauthorized"))
    case code =>
      fromJson[ErrorDescription](response.body) match {
        case Right(errorDescription) => Left(ApiError.RequestFailed(code, errorDescription.errorCode, errorDescription.errorMessage))
        case Left(e) => Left(ApiError.ErrorParseFailed(response.code, e))
      }
  }

  protected def parseApiResponseDiscardBody(response: HttpResponse[String]): Either[ApiError, Unit] = getResponseBody(response).right.map(_ => ())

  protected def parseApiResponse[T](response: HttpResponse[String])(implicit decoder: Decoder[T]): Either[ApiError, T] =
    getResponseBody(response) match {
      case Right(body) =>
        fromJson[T](body) match {
          case Right(result) => Right(result)
          case Left(e) => Left(ApiError.ResultParseFailed(e))
        }
      case Left(e) => Left(e)
    }
}
