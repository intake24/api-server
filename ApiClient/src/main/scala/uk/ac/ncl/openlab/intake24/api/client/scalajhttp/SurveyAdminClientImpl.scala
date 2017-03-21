package uk.ac.ncl.openlab.intake24.api.client.scalajhttp

import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, SurveyAdminClient}
import uk.ac.ncl.openlab.intake24.api.shared.CreateSurveyRequest

class SurveyAdminClientImpl(apiBaseUrl: String) extends SurveyAdminClient with HttpRequestUtil with ApiResponseParser {
  override def createSurvey(accessToken: String, request: CreateSurveyRequest): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthPostRequest(s"$apiBaseUrl/surveys", accessToken, request).asString)


  override def deleteSurvey(accessToken: String, surveyId: String): Either[ApiError, Unit] =
    parseApiResponseDiscardBody(getAuthDeleteRequestNoBody(s"$apiBaseUrl/surveys/$surveyId", accessToken).asString)
}
