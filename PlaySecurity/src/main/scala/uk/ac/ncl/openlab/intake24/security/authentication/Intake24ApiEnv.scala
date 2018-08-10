package uk.ac.ncl.openlab.intake24.security.authentication

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator

trait Intake24ApiEnv extends Env {
  type I = Intake24User
  type A = JWTAuthenticator
}
