package security

import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import play.api.mvc.Request
import security.authorization.AuthenticationService

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class Intake24JWTAuthenticationService @Inject()(silhouette: Environment[Intake24ApiEnv],
                                                 implicit val executionContext: ExecutionContext) extends AuthenticationService[Intake24AuthenticationToken] {

  def parseAuthenticationToken(jwtAuthenticator: Option[JWTAuthenticator]): Option[Intake24AuthenticationToken] =
    jwtAuthenticator.flatMap {
      jwt =>
        (jwt.isValid, jwt.customClaims) match {
          case (true, Some(claims)) =>
            for (tokenType <- (claims \ "type").asOpt[String];
                 userId <- (claims \ "userId").asOpt[Long];
                 token <- tokenType match {
                   case "refresh" => Some(Intake24RefreshToken(userId, jwt))
                   case "access" =>
                     val roles = (claims \ "roles").asOpt[List[String]].getOrElse(List()).toSet
                     Some(Intake24AccessToken(userId, roles, jwt))
                   case _ => None
                 }) yield token
          case _ => None
        }
    }

  def getSubject(request: Request[_]): Future[Option[Intake24AuthenticationToken]] = silhouette.authenticatorService.retrieve(request).map(parseAuthenticationToken)
}