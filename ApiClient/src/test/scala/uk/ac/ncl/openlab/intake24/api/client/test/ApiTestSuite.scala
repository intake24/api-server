package uk.ac.ncl.openlab.intake24.api.client.test

import java.io.BufferedWriter

import org.scalatest.FunSuite
import uk.ac.ncl.openlab.intake24.api.client.ApiError
import uk.ac.ncl.openlab.intake24.api.client.ApiError.{ErrorParseFailed, RequestFailed, ResultParseFailed}
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.SigninClientImpl
import uk.ac.ncl.openlab.intake24.api.shared.SurveyAliasCredentials

trait ApiTestSuite extends FunSuite {

  def assertSuccessful[T](result: Either[ApiError, T]): T =
    result match {
      case Left(ResultParseFailed(cause: Throwable)) => fail(cause)

      case Left(ErrorParseFailed(httpCode, cause)) => fail(cause)

      case Left(RequestFailed(httpCode, errorCode, errorMessage)) => fail(s"API call failed with HTTP code $httpCode: $errorCode: $errorMessage")

      case Right(res) => res
    }

  def assertForbidden(result: Either[ApiError, Unit]) =
    result match {
      case Right(_) => fail("Expected response to be 403 Forbidden, but it was successful")
      case Left(RequestFailed(403, _, _)) => ()
      case Left(RequestFailed(httpCode, _, _)) => fail(s"Expected request to be 403 Forbidden, but got $httpCode instead")
      case Left(ResultParseFailed(cause: Throwable)) => fail(cause)
      case Left(ErrorParseFailed(httpCode, cause)) => fail(s"Could not parse error for code $httpCode", cause)
    }

  val apiBaseUrl = System.getProperty("apiBaseUrl", "http://localhost:9000")

  val signinClient = new SigninClientImpl(apiBaseUrl)

}
