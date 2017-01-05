package uk.ac.ncl.openlab.intake24.api.client

trait ApiError

object ApiError {

  case class ResultParseFailed(cause: Throwable) extends ApiError

  case class ErrorParseFailed(httpCode: Int, cause: Throwable) extends ApiError

  case class RequestFailed(httpCode: Int, errorCode: String, errorMessage: String) extends ApiError
}
