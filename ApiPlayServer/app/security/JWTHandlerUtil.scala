package security

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.{SecurityInfo, User}


trait AuthToken

case class RefreshToken(userInfo: User) extends AuthToken

case class AccessToken(userInfo: User) extends AuthToken

trait JWTHandlerUtil {

  def parseToken(jwtAuthenticator: Option[JWTAuthenticator]): Option[AuthToken] =
    jwtAuthenticator.flatMap {
      jwt =>
        (jwt.isValid, jwt.customClaims) match {
          case (true, Some(claims)) =>
            (claims \ "i24t").asOpt[String] match {
              case Some("refresh") => Some(RefreshToken(User(jwt.loginInfo.providerKey, SecurityInfo(Set(), Set()))))
              case Some("access") =>
                for (roles <- (claims \ "i24r").asOpt[Set[String]];
                     permissions <- (claims \ "i24p").asOpt[Set[String]]
                ) yield AccessToken(User(jwt.loginInfo.providerKey, SecurityInfo(roles, permissions)))
              case _ => None
            }
          case _ => None
        }
    }
}
