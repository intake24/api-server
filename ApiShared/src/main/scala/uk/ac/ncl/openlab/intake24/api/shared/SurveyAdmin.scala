package uk.ac.ncl.openlab.intake24.api.shared

import java.time.ZonedDateTime

case class CreateSurveyRequest(id: String, startDate: ZonedDateTime, endDate: ZonedDateTime, schemeId: String,
                               localeId: String, allowGeneratedUsers: Boolean, externalFollowUpURL: Option[String],
                               supportEmail: String)
