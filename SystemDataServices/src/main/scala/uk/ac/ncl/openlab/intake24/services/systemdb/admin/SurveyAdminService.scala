package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import uk.ac.ncl.openlab.intake24.services.systemdb.errors._

sealed abstract class SurveyState(code: Long)

object SurveyState {

  case object NotInitialised extends SurveyState(0l)

  case object Suspended extends SurveyState(1l)

  case object Active extends SurveyState(2l)

  def fromCode(code: Long): SurveyState = code match {
    case 0l => NotInitialised
    case 1l => Suspended
    case 2l => Active
    case _ => throw new RuntimeException(s"Unexpected survey state code: $code")
  }
}

trait SurveyAdminService {
  def createSurvey(surveyId: String, schemeId: String, localeId: String, allowGeneratedUsers: Boolean, externalFollowUpUrl: Option[String], supportEmail: String): Either[CreateError, Unit]

  def deleteSurvey(surveyId: String): Either[DeleteError, Unit]
}
