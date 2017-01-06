package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, SigninClient}
import uk.ac.ncl.openlab.intake24.api.shared.{AuthToken, Credentials}
import upickle.default._

class SigninClientImpl(apiBaseUrl: String) extends SigninClient with ApiResponseParser with HttpRequestUtil {

  def signin(credentials: Credentials): Either[ApiError, AuthToken] = {
    parseApiResponse[AuthToken](getPostRequest(s"$apiBaseUrl/signin", credentials).asString)
  }
}
