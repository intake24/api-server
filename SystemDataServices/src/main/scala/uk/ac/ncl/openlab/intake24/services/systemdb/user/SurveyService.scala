package uk.ac.ncl.openlab.intake24.services.systemdb.user

import uk.ac.ncl.openlab.intake24.errors._

case class PublicSurveyParameters(localeId: String, supportEmail: String)

case class UserSurveyParameters(schemeId: String, localeId: String, state: String, suspensionReason: Option[String], externalFollowUpURL: Option[String], supportEmail: String)

trait SurveyService {

  def getPublicSurveyParameters(surveyId: String): Either[LookupError, PublicSurveyParameters]

  def getSurveyParameters(surveyId: String): Either[LookupError, UserSurveyParameters]

  //def generateUser(surveyId): Either[GeneratedUserInfo]
}
