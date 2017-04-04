package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, SigninClient}
import uk.ac.ncl.openlab.intake24.api.shared.{SurveyAliasCredentials, RefreshResult, SigninResult}
import upickle.default._

class SigninClientImpl(apiBaseUrl: String) extends SigninClient with ApiResponseParser with HttpRequestUtil {

  def signin(credentials: SurveyAliasCredentials): Either[ApiError, SigninResult] = {
    parseApiResponse[SigninResult](getPostRequest(s"$apiBaseUrl/signin", credentials).asString)
  }

  def refresh(refreshToken: String): Either[ApiError, RefreshResult] = {
    parseApiResponse[RefreshResult](getAuthPostRequestNoBody(s"$apiBaseUrl/refresh", refreshToken).asString)
  }
}
