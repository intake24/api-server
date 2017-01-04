package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, SigninService}
import uk.ac.ncl.openlab.intake24.api.shared.{AuthToken, Credentials}
import upickle.default._

class SigninImpl(apiBaseUrl: String) extends SigninService with ApiResponseParser with HttpRequestUtil {

  def signin(credentials: Credentials): Either[ApiError, AuthToken] = {
    parseApiResponse[AuthToken](getPostRequest(s"$apiBaseUrl/signin", credentials).asString)
  }
}
