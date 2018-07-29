package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.time.{LocalTime, OffsetTime}

import uk.ac.ncl.openlab.intake24.errors.{LookupError, UnexpectedDatabaseError}

case class PendingScheduledExportTask(id: Long, userId: Long, surveyId: String, periodDays: Option[Int], action: String, actionConfig: String)

trait ScheduledDataExportService {

  def createScheduledTask(userId: Long, surveyId: String, periodDays: Option[Int], daysOfWeek: Int, time: LocalTime,
                          timeZoneId: String, action: String, actionConfig: String): Either[UnexpectedDatabaseError, Long]

  def getPendingScheduledTasks(): Either[UnexpectedDatabaseError, Seq[PendingScheduledExportTask]]

  def updateNextRunTime(scheduledTaskId: Long): Either[LookupError, Unit]
}
