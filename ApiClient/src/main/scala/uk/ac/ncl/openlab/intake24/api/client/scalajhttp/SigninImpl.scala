package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, AuthInfo, SigninService}

import scala.concurrent.Future
import scalaj.http._

import scala.concurrent.ExecutionContext.Implicits.global

class SigninImpl(apiBaseUrl: String) extends SigninService with ApiResponseParser {

  def signin(surveyId: String, userName: String, password: String): Future[Either[ApiError, AuthInfo]] = Future {
    parseApiResponse[AuthInfo](Http(s"$apiBaseUrl/signin").asString)
  }
}
