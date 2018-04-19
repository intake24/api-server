package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.time.OffsetTime

import uk.ac.ncl.openlab.intake24.errors.{LookupError, UnexpectedDatabaseError}

case class PendingScheduledExportTask(userId: Long, surveyId: String, periodDays: Option[Int], uploaderName: String, uploaderConfig: String)

trait ScheduledDataExportService {

  def createScheduledTask(userId: Long, surveyId: String, periodDays: Option[Int], daysOfWeek: Int, timeOfDay: OffsetTime,
                          uploaderName: String, uploaderConfig: String): Either[UnexpectedDatabaseError, Long]

  def getPendingScheduledTasks(): Either[UnexpectedDatabaseError, Seq[PendingScheduledExportTask]]

  def updateScheduledRunTime(scheduledTaskId: Long): Either[LookupError, Unit]
}
