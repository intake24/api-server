package security

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models._

trait JWTHandlerUtil {

  def getRefreshSubjectFromJWT(jwtAuthenticator: Option[JWTAuthenticator]): Option[RefreshSubject] =
    getSubjectFromJWT(jwtAuthenticator) match {
      case Some(s@RefreshSubject(_, _)) => Some(s)
      case _ => None
    }

  def getAccessSubjectFromJWT(jwtAuthenticator: Option[JWTAuthenticator]): Option[AccessSubject] =
    getSubjectFromJWT(jwtAuthenticator) match {
      case Some(s@AccessSubject(_, _, _, _)) => Some(s)
      case _ => None
    }

  def getSubjectFromJWT(jwtAuthenticator: Option[JWTAuthenticator]): Option[Intake24Subject] =
    jwtAuthenticator.flatMap {
      jwt =>
        (jwt.isValid, jwt.customClaims) match {
          case (true, Some(claims)) =>
            (claims \ "i24t").asOpt[String] match {
              case Some("refresh") => Some(RefreshSubject(jwt.loginInfo.providerKey, jwt))
              case Some("access") =>
                for (roles <- (claims \ "i24r").asOpt[List[String]];
                     permissions <- (claims \ "i24p").asOpt[List[String]]
                ) yield AccessSubject(jwt.loginInfo.providerKey, roles.map(SecurityRole(_)), permissions.map(SecurityPermission(_)), jwt)
              case _ => None
            }
          case _ => None
        }
    }
}
