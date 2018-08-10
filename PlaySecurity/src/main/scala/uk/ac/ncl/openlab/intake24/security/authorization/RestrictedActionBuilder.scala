package uk.ac.ncl.openlab.intake24.security.authorization

import play.api.mvc._
import uk.ac.ncl.openlab.intake24.security.authentication.AuthenticationService

import scala.concurrent.{ExecutionContext, Future}

trait RestrictedActionBuilder[SubjT, DBErrT] extends Results {

  val actionBuilder: DefaultActionBuilder

  implicit val executionContext: ExecutionContext

  def authenticationService: AuthenticationService[SubjT]

  def failureHandler: AuthorizationFailureHandler[SubjT, DBErrT]

  protected def restrictedAction[BodyT, T](authorizationCheck: SubjT => Option[T],
                                           bodyParser: BodyParser[BodyT],
                                           block: AuthorizedRequest[BodyT, T] => Future[Result]): Action[BodyT] =
    actionBuilder.async(bodyParser) {
      request =>
        authenticationService.getSubject(request).flatMap {
          _ match {
            case None => failureHandler.onAuthenticationFailed
            case Some(subject) =>
              authorizationCheck(subject) match {
                case Some(transformedSubject) => block(AuthorizedRequest(request, transformedSubject))
                case None => failureHandler.onActionNotAuthorized(request, subject)
              }
          }
        }
    }

  protected def restrictedActionWithDatabaseCheck[BodyT, T](authorizationCheck: SubjT => Either[DBErrT, Option[T]], bodyParser: BodyParser[BodyT], block: AuthorizedRequest[BodyT, T] => Future[Result]): Action[BodyT] =
    actionBuilder.async(bodyParser) {
      request =>
        authenticationService.getSubject(request).flatMap {
          _ match {
            case None => failureHandler.onAuthenticationFailed
            case Some(subject) =>
              authorizationCheck(subject) match {
                case Left(dbError) => failureHandler.onDatabaseError(request, subject, dbError)
                case Right(Some(transformedSubject)) => block(AuthorizedRequest(request, transformedSubject))
                case Right(None) => failureHandler.onActionNotAuthorized(request, subject)
              }
          }
        }
    }
}