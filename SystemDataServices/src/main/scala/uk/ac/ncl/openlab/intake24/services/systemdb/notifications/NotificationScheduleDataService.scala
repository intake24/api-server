package uk.ac.ncl.openlab.intake24.services.systemdb.notifications

import java.time.ZonedDateTime

import uk.ac.ncl.openlab.intake24.errors.{DeleteError, UnexpectedDatabaseError}

/**
  * Created by Tim Osadchiy on 22/02/2018.
  */

case class Notification(id: Long, userId: Long, surveyId: Option[String], dateTime: ZonedDateTime, notificationType: String)

object Notification {
  val LoginNotificationType = "login"

  def loginNotification(id: Long, userId: Long, surveyId: Option[String], dateTime: ZonedDateTime) =
    Notification(id, userId, surveyId, dateTime, LoginNotificationType)

}

trait NotificationScheduleDataService {

  def list(): Either[UnexpectedDatabaseError, Seq[Notification]]

  def clean(id: Long): Either[DeleteError, Unit]

}
