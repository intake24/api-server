package uk.ac.ncl.openlab.intake24.security.authentication

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator

sealed trait Intake24AuthenticationToken {
  def userId: Long
  def jwt: JWTAuthenticator
}

case class Intake24RefreshToken(userId: Long, jwt: JWTAuthenticator) extends Intake24AuthenticationToken

case class Intake24AccessToken(userId: Long, roles: Set[String], jwt: JWTAuthenticator) extends Intake24AuthenticationToken