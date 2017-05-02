package uk.ac.ncl.openlab.intake24.api.client

trait AuthCache {

  def withAccessToken[T](block: String => Either[ApiError, T]): Either[ApiError, T]
}
