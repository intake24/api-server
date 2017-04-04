package security

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models._

trait JWTHandlerUtil {

  def getRefreshSubjectFromJWT(jwtAuthenticator: Option[JWTAuthenticator]): Option[RefreshSubject] =
    getSubjectFromJWT(jwtAuthenticator).flatMap {
      case s: RefreshSubject => Some(s)
      case _ => None
    }

  def getAccessSubjectFromJWT(jwtAuthenticator: Option[JWTAuthenticator]): Option[AccessSubject] =
    getSubjectFromJWT(jwtAuthenticator).flatMap {
      case s: AccessSubject => Some(s)
      case _ => None
    }

  def getSubjectFromJWT(jwtAuthenticator: Option[JWTAuthenticator]): Option[Intake24Subject] =
    jwtAuthenticator.flatMap {
      jwt =>
        (jwt.isValid, jwt.customClaims) match {
          case (true, Some(claims)) =>

            val common = for (t <- (claims \ "type").asOpt[String];
                              userId <- (claims \ "userId").asOpt[Long])
              yield (t, userId)

            common match {
              case Some(("refresh", userId)) =>
                Some(RefreshSubject(jwt.loginInfo.providerKey, userId, jwt))
              case Some(("access", userId)) =>
                (claims \ "roles").asOpt[List[String]].map {
                  roles =>
                    AccessSubject(jwt.loginInfo.providerKey, userId, roles.toSet, jwt)
                }

              case _ => None
            }
          case _ => None
        }
    }
}
