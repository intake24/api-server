package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.api.shared.{Credentials, RefreshResult, SigninResult}

trait SigninClient {

  def signin(credentials: Credentials): Either[ApiError, SigninResult]

  def refresh(refreshToken: String): Either[ApiError, RefreshResult]
}
