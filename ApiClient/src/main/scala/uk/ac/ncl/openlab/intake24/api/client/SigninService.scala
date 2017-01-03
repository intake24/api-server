package uk.ac.ncl.openlab.intake24.api.client

import scala.concurrent.Future

case class AuthInfo(token: String)

trait SigninService {

  def signin(surveyId: String, userName: String, password: String): Future[Either[ApiError, AuthInfo]]

}
