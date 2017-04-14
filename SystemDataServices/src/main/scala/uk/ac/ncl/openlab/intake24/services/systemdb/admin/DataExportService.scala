package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.time.{Instant, ZonedDateTime}
import java.util.UUID

import uk.ac.ncl.openlab.intake24.errors.LookupError
import uk.ac.ncl.openlab.intake24.surveydata.{MealTime, PortionSize}


case class ExportSubmission(id: UUID, userId: Int, userAlias: Option[String], userCustomData: Map[String, String], surveyCustomData: Map[String, String], startTime: ZonedDateTime, endTime: ZonedDateTime, meals: Seq[ExportMeal])

case class ExportMeal(name: String, time: MealTime, customData: Map[String, String], foods: Seq[ExportFood])

case class ExportFood(code: String, englishDescription: String, localDescription: Option[String], searchTerm: String, nutrientTableId: String, nutrientTableCode: String, isReadyMeal: Boolean,
                      portionSize: PortionSize, reasonableAmount: Boolean, foodGroupId: Int, brand: String, nutrients: Map[Int, Double], customData: Map[String, String])


trait DataExportService {

  def getSurveySubmissions(surveyId: String, dateFrom: Option[ZonedDateTime], dateTo: Option[ZonedDateTime], offset: Int, limit: Int, respondentId: Option[Long]): Either[LookupError, Seq[ExportSubmission]]

  //def getSurveySubmissionsAsCSV()

  //def getActivityReportAsJSON()

  //def getActivityReportAsCSV()
}

