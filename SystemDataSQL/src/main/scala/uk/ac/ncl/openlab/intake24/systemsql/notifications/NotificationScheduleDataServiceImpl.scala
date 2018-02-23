package uk.ac.ncl.openlab.intake24.systemsql.notifications

import java.time.ZonedDateTime

import anorm.{Macro, SQL}
import com.google.inject.Inject
import javax.inject.Named
import javax.sql.DataSource
import uk.ac.ncl.openlab.intake24.errors.{DeleteError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.systemdb.notifications.{Notification, NotificationScheduleDataService}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}

/**
  * Created by Tim Osadchiy on 22/02/2018.
  */
class NotificationScheduleDataServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends NotificationScheduleDataService with SqlDataService with SqlResourceLoader {

  private case class NotificationRow(id: Long, user_id: Long, survey_id: Option[String], datetime: ZonedDateTime, notification_type: String)

  private val tableName = "user_notification_schedule"
  private val listQ = s"SELECT id, user_id, survey_id, datetime, notification_type FROM $tableName;"
  private val deleteQ = s"DELETE FROM user_notification_schedule WHERE id = {id};"

  override def list(): Either[UnexpectedDatabaseError, Seq[Notification]] = tryWithConnection {
    implicit conn =>
      val r = SQL(listQ).executeQuery().as(Macro.namedParser[NotificationRow].*)
        .map(r => Notification(r.id, r.user_id, r.survey_id, r.datetime, r.notification_type))
      Right(r)
  }

  override def clean(id: Long): Either[DeleteError, Unit] = tryWithConnection {
    implicit conn =>
      SQL(deleteQ).on('id -> id).execute()
      Right(())
  }

}
