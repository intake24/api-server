package uk.ac.ncl.openlab.intake24.api.shared

case class CreateSurveyRequest(id: String, schemeId: String, localeId: String, allowGeneratedUsers: Boolean, externalFollowUpURL: Option[String], supportEmail: String)
