package uk.ac.ncl.openlab.intake24.api.client.test

import org.scalatest.FunSuite
import uk.ac.ncl.openlab.intake24.api.client.ApiError
import uk.ac.ncl.openlab.intake24.api.client.ApiError.{ErrorParseFailed, RequestFailed, ResultParseFailed}
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.SigninClientImpl
import uk.ac.ncl.openlab.intake24.api.shared.Credentials

trait ApiTestSuite extends FunSuite {

  def ensureSuccessful[T](result: Either[ApiError, T]): T =
    result match {
      case Left(ResultParseFailed(cause: Throwable)) => fail(cause)

      case Left(ErrorParseFailed(httpCode, cause)) => fail(cause)

      case Left(RequestFailed(httpCode, errorCode, errorMessage)) => fail(s"API call failed with HTTP code $httpCode: $errorCode: $errorMessage")

      case Right(res) => res
    }

  val apiBaseUrl = "http://localhost:9000"

  val signinClient = new SigninClientImpl(apiBaseUrl)

  var refreshToken: String = null
  var accessToken: String = null

  test("Sign in with credentials and get the refresh token") {
    val result = ensureSuccessful(signinClient.signin(Credentials("", "admin", "intake24")))
    refreshToken = result.refreshToken
  }

  test("Get an access token") {
    val result = ensureSuccessful(signinClient.refresh(refreshToken))
    refreshToken = result.refreshToken
    accessToken = result.accessToken
  }
}
