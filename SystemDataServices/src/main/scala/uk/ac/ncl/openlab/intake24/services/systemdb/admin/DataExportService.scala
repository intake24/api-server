package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.time.Instant

import uk.ac.ncl.openlab.intake24.errors.LookupError
import uk.ac.ncl.openlab.intake24.surveydata.NutrientMappedSubmission

trait DataExportService {

  def getSurveySubmissions(surveyId: String, dateFrom: Option[Instant], dateTo: Option[Instant], offset: Int, limit: Int, respondentId: Option[String]): Either[LookupError, Seq[NutrientMappedSubmission]]

  //def getSurveySubmissionsAsCSV()

  //def getActivityReportAsJSON()

  //def getActivityReportAsCSV()
}

