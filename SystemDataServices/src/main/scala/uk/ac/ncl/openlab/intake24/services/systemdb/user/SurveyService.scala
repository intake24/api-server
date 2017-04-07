package uk.ac.ncl.openlab.intake24.services.systemdb.user

import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.surveydata.NutrientMappedSubmission

case class PublicSurveyParameters(localeId: String, supportEmail: String)

case class UserSurveyParameters(schemeId: String, localeId: String, state: String, suspensionReason: Option[String], externalFollowUpURL: Option[String], supportEmail: String)

trait SurveyService {

  def getPublicSurveyParameters(surveyId: String): Either[LookupError, PublicSurveyParameters]

  def getSurveyParameters(surveyId: String): Either[LookupError, UserSurveyParameters]

  def createSubmission(userId: Long, surveyId: String, submission: NutrientMappedSubmission): Either[UnexpectedDatabaseError, Unit]
}
