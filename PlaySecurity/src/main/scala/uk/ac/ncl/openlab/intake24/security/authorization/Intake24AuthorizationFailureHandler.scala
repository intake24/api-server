package uk.ac.ncl.openlab.intake24.security.authorization

import play.api.mvc.{Request, Result, Results}
import uk.ac.ncl.openlab.intake24.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.play.utils.DatabaseErrorHandler
import uk.ac.ncl.openlab.intake24.security.authentication.Intake24AuthenticationToken

import scala.concurrent.Future

class Intake24AuthorizationFailureHandler extends AuthorizationFailureHandler[Intake24AuthenticationToken, DatabaseError] with Results with DatabaseErrorHandler {

  def onAuthenticationFailed: Future[Result] = Future.successful(Unauthorized)

  def onDatabaseError[A](request: Request[A], subject: Intake24AuthenticationToken, error: DatabaseError) = Future.successful(translateDatabaseError(error))

  def onActionNotAuthorized[A](request: Request[A], subject: Intake24AuthenticationToken): Future[Result] = Future.successful(Forbidden)
}