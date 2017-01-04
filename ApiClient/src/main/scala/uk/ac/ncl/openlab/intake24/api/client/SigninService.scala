package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.api.shared.{AuthToken, Credentials}

trait SigninService {

  def signin(credentials: Credentials): Either[ApiError, AuthToken]
}
