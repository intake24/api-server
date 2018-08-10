package uk.ac.ncl.openlab.intake24.security.authorization

import play.api.mvc.{Request, Result}

import scala.concurrent.Future

trait AuthorizationFailureHandler[SubjT, DBErrT] {

  // User could not be authenticated, e.g. authentication token is missing or invalid
  def onAuthenticationFailed: Future[Result]

  // User has been authenticated, but lacks the permission to perform an action
  def onActionNotAuthorized[A](request: Request[A], subject: SubjT): Future[Result]

  def onDatabaseError[A](request: Request[A], subject: SubjT, databaseError: DBErrT): Future[Result]
}