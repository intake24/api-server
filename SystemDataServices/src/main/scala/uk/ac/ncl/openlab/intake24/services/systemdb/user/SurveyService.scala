package uk.ac.ncl.openlab.intake24.services.systemdb.user

import uk.ac.ncl.openlab.intake24.errors._

case class PublicSurveyParameters(localeId: String)

trait SurveyService {

  def getPublicSurveyParameters(surveyId: String): Either[LookupError, PublicSurveyParameters]

  //def generateUser(surveyId): Either[GeneratedUserInfo]
}
