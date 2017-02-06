package security

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.Intake24User
import com.mohiva.play.silhouette.api.Env

trait Intake24ApiEnv extends Env {
  type I = Intake24User
  type A = JWTAuthenticator
}
