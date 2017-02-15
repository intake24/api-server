package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.time.{Instant, LocalDateTime}

import uk.ac.ncl.openlab.intake24.services.systemdb.errors._
import uk.ac.ncl.openlab.intake24.surveydata.NutrientMappedSubmission

trait DataExportService {

  def getSurveySubmissions(surveyId: String, dateFrom: Instant, dateTo: Instant, offset: Int, limit: Int): Either[UnexpectedDatabaseError, Seq[NutrientMappedSubmission]]

  //def getSurveySubmissionsAsCSV()

  //def getActivityReportAsJSON()

  //def getActivityReportAsCSV()

}
