package uk.ac.ncl.openlab.intake24.systemsql.admin

import java.time.{OffsetTime, ZonedDateTime}
import javax.inject.{Inject, Named}
import javax.sql.DataSource

import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.errors.{LookupError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.services.systemdb.admin._
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

class ScheduledDataExportImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends ScheduledDataExportService with SqlDataService {

  val logger = LoggerFactory.getLogger(classOf[ScheduledDataExportImpl])

  private def getNextRunTime(daysOfWeek: Int, timeOfDay: OffsetTime): ZonedDateTime = {
    // Find next day of week that is >= today
  }

  def createScheduledTask(userId: Long, surveyId: String, periodDays: Option[Int], daysOfWeek: Int, timeOfDay: OffsetTime,
                          uploaderName: String, uploaderConfig: String): Either[UnexpectedDatabaseError, Long] = ???

  def getPendingScheduledTasks(): Either[UnexpectedDatabaseError, Seq[PendingScheduledExportTask]] = ???

  def updateScheduledRunTime(scheduledTaskId: Long): Either[LookupError, Unit] = ???
}
