package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.api.shared.{EmailCredentials, RefreshResult, SigninResult, SurveyAliasCredentials}

trait SigninClient {

  def signin(credentials: EmailCredentials): Either[ApiError, SigninResult]

  def signinWithAlias(credentials: SurveyAliasCredentials): Either[ApiError, SigninResult]

  def refresh(refreshToken: String): Either[ApiError, RefreshResult]
}
