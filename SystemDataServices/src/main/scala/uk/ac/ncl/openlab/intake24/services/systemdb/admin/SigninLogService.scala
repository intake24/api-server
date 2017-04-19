package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import uk.ac.ncl.openlab.intake24.errors.UnexpectedDatabaseError

case class SigninAttempt(remoteAddress: String, provider: String, providerKey: String, success: Boolean, userId: Option[Long], message: Option[String])

trait SigninLogService {

  def logSigninAttempt(event: SigninAttempt): Either[UnexpectedDatabaseError, Unit]
}
