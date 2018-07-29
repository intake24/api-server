package uk.ac.ncl.openlab.intake24.services.systemdb.user

import java.time.ZonedDateTime

import uk.ac.ncl.openlab.intake24.errors.{CreateError, DeleteError, LookupError}

/**
  * Created by Tim Osadchiy on 21/03/2018.
  */

case class UserSession(userId: Long, surveyId: String, sessionData: String, created: ZonedDateTime)

object UserSession {
  def apply(userId: Long, sessionId: String, sessionData: String): UserSession = UserSession(userId, sessionId, sessionData, ZonedDateTime.now())
}

trait UserSessionDataService {

  def save(userSession: UserSession): Either[CreateError, UserSession]

  def get(surveyId: String, userId: Long): Either[LookupError, UserSession]

  def clean(surveyId: String, userId: Long): Either[DeleteError, Unit]

}
