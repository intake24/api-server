package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.api.shared.CreateSurveyRequest


trait SurveyAdminClient {
  def createSurvey(accessToken: String, request: CreateSurveyRequest): Either[ApiError, Unit]

  def deleteSurvey(accessToken: String, surveyId: String): Either[ApiError, Unit]
}
