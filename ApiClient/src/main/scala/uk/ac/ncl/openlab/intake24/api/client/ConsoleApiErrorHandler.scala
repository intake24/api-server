package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.api.client.ApiError.{ErrorParseFailed, RequestFailed, ResultParseFailed}

trait ConsoleApiErrorHandler {

  def checkApiError(result: Either[ApiError, Unit]) = result match {
    case Right(()) => println("Success!")
    case Left(RequestFailed(httpCode, errorCode, errorMessage)) =>
      println("API request failed!")
      println(s"  HTTP code: $httpCode")
      println(s"  Error code: $errorCode")
      println(s"  Error message: $errorMessage")
    case Left(ResultParseFailed(cause: Throwable)) =>
      println("API call succeeded, but failed to parse result!")
      println(s"  Parser error: ${cause.getClass.getSimpleName}: ${cause.getMessage}")
    case Left(ErrorParseFailed(httpCode, cause: Throwable)) =>
      println("API request failed, but did not return error information in expected format!")
      println(s"  HTTP code: $httpCode")
      println(s"  Parser error: ${cause.getClass.getSimpleName}: ${cause.getMessage}")
  }
}
