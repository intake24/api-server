package uk.ac.ncl.openlab.intake24.api.shared

case class CreateSurveyRequest(surveyId: String, schemeId: String, localeId: String, allowGeneratedUsers: Boolean, externalFollowUpUrl: Option[String], supportEmail: String)
