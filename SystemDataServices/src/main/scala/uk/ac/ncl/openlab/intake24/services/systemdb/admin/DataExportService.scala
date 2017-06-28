package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.time.{Instant, ZonedDateTime}
import java.util.UUID

import uk.ac.ncl.openlab.intake24.errors.{LookupError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.surveydata.{MealTime, MissingFood, PortionSize}


case class ExportSubmission(id: UUID, userId: Int, userAlias: Option[String], userCustomData: Map[String, String], surveyCustomData: Map[String, String], startTime: ZonedDateTime, endTime: ZonedDateTime, meals: Seq[ExportMeal])

case class ExportMeal(name: String, time: MealTime, customData: Map[String, String], foods: Seq[ExportFood], missingFoods: Seq[MissingFood])

case class ExportFood(code: String, englishDescription: String, localDescription: Option[String], searchTerm: String, nutrientTableId: String, nutrientTableCode: String, isReadyMeal: Boolean,
                      portionSize: PortionSize, reasonableAmount: Boolean, foodGroupId: Int, brand: String, nutrients: Map[Int, Double], customData: Map[String, String])


case class ExportTaskParameters(userId: Long, surveyId: String, dateFrom: ZonedDateTime, dateTo: ZonedDateTime)

case class ExportTaskProgressUpdate(id: Long, progress: Double)

case class ExportTaskSuccess(id: Long, downloadUrl: String)

case class ExportTaskFailure(id: Long, cause: Throwable)


case class ExportTaskStatus(id: Long, progress: Option[Double], status: Option[Boolean], downloadUrl: Option[String])

trait DataExportService {

  def getSurveySubmissions(surveyId: String, dateFrom: Option[ZonedDateTime], dateTo: Option[ZonedDateTime], offset: Int, limit: Int, respondentId: Option[Long]): Either[LookupError, Seq[ExportSubmission]]

  def createExportTask(parameters: ExportTaskParameters): Either[UnexpectedDatabaseError, Long]

  def setExportTaskStarted(taskId: Long): Either[LookupError, Unit]

  def updateExportTaskProgress(taskId: Long, progress: Double): Either[LookupError, Unit]

  def setExportTaskSuccess(taskId: Long, downloadUrl: String): Either[LookupError, Unit]

  def setExportTaskFailure(taskId: Long, cause: Throwable): Either[LookupError, Unit]

  def getExportTaskStatus(taskId: Long): Either[LookupError, ExportTaskStatus]

  //def getSurveySubmissionsAsCSV()

  //def getActivityReportAsJSON()

  //def getActivityReportAsCSV()
}

