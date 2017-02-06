package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.api.shared.{SigninResult, Credentials}

trait SigninClient {

  def signin(credentials: Credentials): Either[ApiError, SigninResult]
}
