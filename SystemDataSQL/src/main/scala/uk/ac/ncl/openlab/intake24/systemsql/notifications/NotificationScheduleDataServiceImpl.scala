package uk.ac.ncl.openlab.intake24.systemsql.notifications

import java.time.ZonedDateTime

import anorm.{BatchSql, Macro, NamedParameter, SQL}
import com.google.inject.Inject
import javax.inject.Named
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.errors.{CreateError, DeleteError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.systemdb.notifications.{NewNotification, Notification, NotificationScheduleDataService}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

/**
  * Created by Tim Osadchiy on 22/02/2018.
  */
class NotificationScheduleDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends NotificationScheduleDataService with SqlDataService with SqlResourceLoader {

  private case class NotificationRow(id: Long, user_id: Long, survey_id: Option[String], datetime: ZonedDateTime, notification_type: String) {
    def toNotification() = Notification(id, user_id, survey_id, datetime, notification_type)
  }

  private val tableName = "user_notification_schedule"
  private val listQ = s"SELECT id, user_id, survey_id, datetime, notification_type FROM $tableName;"
  private val createQ =
    s"""
       |INSERT INTO user_notification_schedule (user_id, survey_id, datetime, notification_type)
       |VALUES ({user_id}, {survey_id}, {datetime}, {notification_type})
       |RETURNING id, user_id, survey_id, datetime, notification_type
     """.stripMargin
  private val deleteQ = s"DELETE FROM user_notification_schedule WHERE id = {id};"

  override def list(): Either[UnexpectedDatabaseError, Seq[Notification]] = tryWithConnection {
    implicit conn =>
      val r = SQL(listQ).executeQuery().as(Macro.namedParser[NotificationRow].*).map(r => r.toNotification())
      Right(r)
  }

  override def create(notification: NewNotification): Either[CreateError, Notification] = tryWithConnection {
    implicit conn =>
      val r = SQL(createQ).on('user_id -> notification.userId, 'survey_id -> notification.surveyId,
        'datetime -> notification.dateTime, 'notification_type -> notification.notificationType)
        .executeQuery().as(Macro.namedParser[NotificationRow].single)
      Right(r.toNotification())
  }

  override def batchCreate(notifications: Seq[NewNotification]): Either[CreateError, Unit] = tryWithConnection {
    implicit conn =>
      val parameters = notifications.map { n =>
        Seq[NamedParameter]('user_id -> n.userId, 'survey_id -> n.surveyId,
          'datetime -> n.dateTime, 'notification_type -> n.notificationType)
      }
      BatchSql(createQ, parameters.head, parameters.tail: _*).execute()
      Right(())
  }

  override def clean(id: Long): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      SQL(deleteQ).on('id -> id).execute()
      Right(())
  }

}
