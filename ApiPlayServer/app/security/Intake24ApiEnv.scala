package security

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User
import com.mohiva.play.silhouette.api.Env

trait Intake24ApiEnv extends Env {
  type I = User
  type A = JWTAuthenticator
}
