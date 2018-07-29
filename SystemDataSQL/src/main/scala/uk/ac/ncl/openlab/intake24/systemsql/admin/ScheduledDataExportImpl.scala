package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Named, Singleton}
import javax.sql.DataSource

import anorm.Macro.ColumnNaming
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{LookupError, RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.sql.SqlDataService
import anorm.{Macro, SQL, SqlParser, ~}

@Singleton
class ScheduledDataExportImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends ScheduledDataExportService with SqlDataService {

  private val logger = LoggerFactory.getLogger(classOf[ScheduledDataExportImpl])

  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  private def nextRunAt(daysOfWeek: Int, time: LocalTime, tz: ZoneId): Option[LocalDateTime] = {

    def tryNextDate(candidate: ZonedDateTime, offset: Int): Option[ZonedDateTime] =
      if (offset > 7)
        None
      else {
        val nowThere = ZonedDateTime.now(tz)
        val dow = candidate.getDayOfWeek.getValue - 1

        if (nowThere.compareTo(candidate) < 0 && (daysOfWeek & (1 << dow)) != 0)
          Some(candidate)
        else
          tryNextDate(candidate.plus(1, ChronoUnit.DAYS), offset + 1)
      }

    val localRunAt = tryNextDate(ZonedDateTime.of(LocalDate.now(tz), time, tz), 0).map(_.toOffsetDateTime)

    localRunAt.map(_.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime)
  }

  def createScheduledTask(userId: Long, surveyId: String, periodDays: Option[Int], daysOfWeek: Int, time: LocalTime, timeZoneId: String,
                          action: String, actionConfig: String): Either[UnexpectedDatabaseError, Long] = tryWithConnection {
    implicit conn =>
      val nextRunTime = nextRunAt(daysOfWeek, time, ZoneId.of(timeZoneId))

      nextRunTime match {
        case Some(runTime) =>
          val id: Option[Long] = SQL(
            """INSERT INTO data_export_scheduled(user_id, survey_id, period_days, days_of_week, time, time_zone, action, action_config, next_run_utc)
              |VALUES({user_id},{survey_id},{period_days},{days_of_week},{time}::time,{time_zone},{action},{action_config},{next_run_utc})""".stripMargin)
            .on('user_id -> userId, 'survey_id -> surveyId, 'period_days -> periodDays, 'days_of_week -> daysOfWeek,
              'time -> time.format(timeFormatter), 'time_zone -> timeZoneId, 'action -> action, 'action_config -> actionConfig,
              'next_run_utc -> runTime)
            .executeInsert()

          id match {
            case Some(id) => Right(id)
            case None => Left(UnexpectedDatabaseError(new RuntimeException("insert failed")))
          }
        case None =>
          Left(UnexpectedDatabaseError(new RuntimeException("Cannot find a suitable run time for task, check days of week setting")))
      }
  }

  def getPendingScheduledTasks(): Either[UnexpectedDatabaseError, Seq[PendingScheduledExportTask]] = tryWithConnection {
    implicit conn =>
      Right(SQL(
        """SELECT id, user_id, survey_id, period_days, action, action_config
           FROM data_export_scheduled
           WHERE now() AT TIME ZONE 'utc' > next_run_utc ORDER BY next_run_utc
        """.stripMargin)
        .executeQuery()
        .as(Macro.namedParser[PendingScheduledExportTask](ColumnNaming.SnakeCase).*))
  }

  def updateNextRunTime(scheduledTaskId: Long): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {

        val taskParams = SQL("SELECT days_of_week, time::text, time_zone FROM data_export_scheduled WHERE id = {task_id}")
          .on('task_id -> scheduledTaskId)
          .executeQuery()
          .as((SqlParser.int(1) ~ SqlParser.str(2) ~ SqlParser.str(3)).singleOpt)

        taskParams match {
          case Some(daysOfWeek ~ timeStr ~ timeZoneId) =>

            val time = LocalTime.parse(timeStr)
            val nextRunTime = nextRunAt(daysOfWeek, time, ZoneId.of(timeZoneId))

            nextRunTime match {
              case Some(time) =>
                SQL("UPDATE data_export_scheduled SET next_run_utc={next_run} WHERE id={task_id}")
                  .on('next_run -> nextRunTime, 'task_id -> scheduledTaskId)
                  .executeUpdate()
                Right(())
              case None =>
                Left(UnexpectedDatabaseError(new RuntimeException(s"Cannot find a suitable run time for scheduled export task $scheduledTaskId, check days of week setting")))
            }

          case None => Left(RecordNotFound(new RuntimeException(s"Task $scheduledTaskId not found")))
        }
      }
  }
}
