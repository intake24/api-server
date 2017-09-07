package security

import com.google.inject.{Inject, Singleton}
import play.api.mvc._
import security.authorization.{AuthorizedRequest, RestrictedActionBuilder}
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class Intake24RestrictedActionBuilder @Inject()(val authenticationService: Intake24JWTAuthenticationService,
                                                val failureHandler: Intake24AuthorizationFailureHandler,
                                                val playBodyParsers: PlayBodyParsers,
                                                val actionBuilder: DefaultActionBuilder,
                                                implicit val executionContext: ExecutionContext) extends RestrictedActionBuilder[Intake24AuthenticationToken, AnyError] {

  private def checkIfAccessToken(token: Intake24AuthenticationToken, check: Intake24AccessToken => Boolean) = token match {
    case t: Intake24AccessToken => if (check(t)) Some(t) else None
    case _ => None
  }

  private def checkIfRefreshToken(token: Intake24AuthenticationToken, check: Intake24RefreshToken => Boolean) = token match {
    case t: Intake24RefreshToken => if (check(t)) Some(t) else None
    case _ => None
  }

  private def dbCheckIfAccessToken(token: Intake24AuthenticationToken, check: Intake24AccessToken => Either[AnyError, Boolean]) = token match {
    case t: Intake24AccessToken => check(t).right.map(authorized => if (authorized) Some(t) else None)
    case _ => Right(None)
  }


  class RestrictAccessWithDatabaseCheckAdapter(check: Intake24AccessToken => Either[AnyError, Boolean]) {
    def apply[A](bodyParser: BodyParser[A])(block: AuthorizedRequest[A, Intake24AccessToken] => Future[Result]) =
      restrictedActionWithDatabaseCheck[A, Intake24AccessToken](t => dbCheckIfAccessToken(t, check), bodyParser, block)

    def apply(block: AuthorizedRequest[Unit, Intake24AccessToken] => Future[Result]) =
      restrictedActionWithDatabaseCheck[Unit, Intake24AccessToken](t => dbCheckIfAccessToken(t, check), playBodyParsers.empty, block)

    def apply(block: => Future[Result]) =
      restrictedActionWithDatabaseCheck[Unit, Intake24AccessToken](t => dbCheckIfAccessToken(t, check), playBodyParsers.empty, _ => block)
  }

  class RestrictAccessAdapter(check: Intake24AccessToken => Boolean) {
    def apply[A](bodyParser: BodyParser[A])(block: AuthorizedRequest[A, Intake24AccessToken] => Future[Result]) =
      restrictedAction[A, Intake24AccessToken](t => checkIfAccessToken(t, check), bodyParser, block)

    def apply(block: AuthorizedRequest[Unit, Intake24AccessToken] => Future[Result]) =
      restrictedAction[Unit, Intake24AccessToken](t => checkIfAccessToken(t, check), playBodyParsers.empty, block)

    def apply(block: => Future[Result]) =
      restrictedAction[Unit, Intake24AccessToken](t => checkIfAccessToken(t, check), playBodyParsers.empty, _ => block)
  }

  def restrictRefresh(block: AuthorizedRequest[Unit, Intake24RefreshToken] => Future[Result]) =
    restrictedAction[Unit, Intake24RefreshToken](t => checkIfRefreshToken(t, _ => true), playBodyParsers.empty, block)

  def restrictAccess(check: Intake24AccessToken => Boolean) = new RestrictAccessAdapter(check)

  def restrictAccessWithDatabaseCheck(check: Intake24AccessToken => Either[AnyError, Boolean]) = new RestrictAccessWithDatabaseCheckAdapter(check)


  def restrictToAuthenticated = restrictAccess(_ => true)

  def restrictToRoles(roles: String*) = restrictAccess(t => roles.exists(r => t.roles.contains(r)))

  def restrictToRespondents = restrictAccess(t => t.roles.exists(r => r.endsWith(Roles.respondentSuffix)))
}
