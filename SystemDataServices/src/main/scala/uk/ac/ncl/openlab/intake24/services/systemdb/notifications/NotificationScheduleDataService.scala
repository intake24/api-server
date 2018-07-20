package uk.ac.ncl.openlab.intake24.services.systemdb.notifications

import java.time.ZonedDateTime

import uk.ac.ncl.openlab.intake24.errors.{CreateError, DeleteError, UnexpectedDatabaseError}

/**
  * Created by Tim Osadchiy on 22/02/2018.
  */

case class Notification(id: Long, userId: Long, surveyId: Option[String], dateTime: ZonedDateTime, notificationType: String)
case class NewNotification(userId: Long, surveyId: Option[String], dateTime: ZonedDateTime, notificationType: String)

object Notification {
  val NotificationTypeLoginRecallForYesterday = "login-recall-yesterday"
  val NotificationTypeLoginRecallForToday = "login-recall-today"
  val NotificationTypeLoginBackToRecall = "login-back-recall"
  val NotificationTypeLoginLast = "login-last"
  val NotificationTypeLoginFirstReminder = "login-first-reminder"
  val NotificationTypeLoginSecondReminder = "login-second-reminder"
}

case class RecallNotificationRequest(userEmail: String, dateTime: ZonedDateTime, notificationType: String)

trait NotificationScheduleDataService {

  def list(): Either[UnexpectedDatabaseError, Seq[Notification]]

  def create(notification: NewNotification): Either[CreateError, Notification]

  def batchCreate(notifications: Seq[NewNotification]): Either[CreateError, Unit]

  def clean(id: Long): Either[DeleteError, Unit]

}
