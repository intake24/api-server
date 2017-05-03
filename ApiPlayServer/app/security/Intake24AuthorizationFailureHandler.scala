package security

import controllers.DatabaseErrorHandler
import play.api.mvc.{Request, Result, Results}
import security.authorization.AuthorizationFailureHandler
import uk.ac.ncl.openlab.intake24.errors.AnyError

import scala.concurrent.Future

class Intake24AuthorizationFailureHandler extends AuthorizationFailureHandler[Intake24AuthenticationToken, AnyError] with Results with DatabaseErrorHandler {

  def onAuthenticationFailed: Future[Result] = Future.successful(Unauthorized)

  def onDatabaseError[A](request: Request[A], subject: Intake24AuthenticationToken, error: AnyError) = Future.successful(translateDatabaseError(error))

  def onActionNotAuthorized[A](request: Request[A], subject: Intake24AuthenticationToken): Future[Result] = Future.successful(Forbidden)
}