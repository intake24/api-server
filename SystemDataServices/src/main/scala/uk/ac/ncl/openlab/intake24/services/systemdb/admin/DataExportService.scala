package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.net.URL
import java.time.ZonedDateTime
import java.util.UUID

import uk.ac.ncl.openlab.intake24.errors.{LookupError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.surveydata.{MealTime, MissingFood, PortionSizeWithWeights}


case class ExportSubmission(id: UUID, userId: Int, userAlias: Option[String], userCustomData: Map[String, String], surveyCustomData: Map[String, String], startTime: ZonedDateTime, endTime: ZonedDateTime, meals: Seq[ExportMeal])

case class ExportMeal(name: String, time: MealTime, customData: Map[String, String], foods: Seq[ExportFood], missingFoods: Seq[MissingFood])

case class ExportFood(code: String, englishDescription: String, localDescription: Option[String], searchTerm: String, nutrientTableId: String, nutrientTableCode: String, isReadyMeal: Boolean,
                      portionSize: PortionSizeWithWeights, reasonableAmount: Boolean, foodGroupId: Int, brand: String, nutrients: Map[Int, Double], customData: Map[String, String])

case class ExportTaskProgressUpdate(id: Long, progress: Double)

case class ExportTaskSuccess(id: Long, downloadUrl: String)

case class ExportTaskFailure(id: Long, cause: Throwable)


sealed trait ExportTaskStatus

object ExportTaskStatus {

  case object Pending extends ExportTaskStatus

  case object Failed extends ExportTaskStatus

  case class InProgress(progress: Double) extends ExportTaskStatus

  case object DownloadUrlPending extends ExportTaskStatus

  case class DownloadUrlAvailable(url: String) extends ExportTaskStatus

  case object UploadPending extends ExportTaskStatus
}

case class ExportTaskInfo(id: Long, createdAt: ZonedDateTime, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, status: ExportTaskStatus)

trait DataExportService {

  def getSurveySubmissions(surveyId: String, dateFrom: Option[ZonedDateTime], dateTo: Option[ZonedDateTime], offset: Int, limit: Int, respondentId: Option[Long]): Either[LookupError, Seq[ExportSubmission]]

  def getSurveySubmissionCount(surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime): Either[LookupError, Int]

  def createExportTask(userId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime, purpose: String): Either[UnexpectedDatabaseError, Long]

  def setExportTaskStarted(taskId: Long): Either[LookupError, Unit]

  def updateExportTaskProgress(taskId: Long, progress: Double): Either[LookupError, Unit]

  def setExportTaskSuccess(taskId: Long): Either[LookupError, Unit]

  def setExportTaskFailure(taskId: Long, cause: Throwable): Either[LookupError, Unit]

  def getActiveExportTasks(surveyId: String, userId: Long): Either[LookupError, Seq[ExportTaskInfo]]

  def setExportTaskDownloadUrl(taskId: Long, url: URL, expiresAt: ZonedDateTime): Either[LookupError, Unit]

  def setExportTaskDownloadFailed(taskId: Long, cause: Throwable): Either[LookupError, Unit]

  //def getSurveySubmissionsAsCSV()

  //def getActivityReportAsJSON()

  //def getActivityReportAsCSV()
}
