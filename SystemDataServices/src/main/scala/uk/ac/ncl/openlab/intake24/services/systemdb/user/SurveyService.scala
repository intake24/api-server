package uk.ac.ncl.openlab.intake24.services.systemdb.user

import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.surveydata.NutrientMappedSubmission

case class PublicSurveyParameters(localeId: String, supportEmail: String, originatingURL: Option[String])

case class UserSurveyParameters(schemeId: String, localeId: String, state: String, suspensionReason: Option[String], description: Option[String])

case class SurveyFollowUp(followUpUrl: Option[String], showFeedback: Boolean)

trait SurveyService {

  def getPublicSurveyParameters(surveyId: String): Either[LookupError, PublicSurveyParameters]

  def getSurveyParameters(surveyId: String): Either[LookupError, UserSurveyParameters]

  def getSurveyFollowUp(surveyId: String): Either[LookupError, SurveyFollowUp]

  def createSubmission(userId: Long, surveyId: String, submission: NutrientMappedSubmission): Either[UnexpectedDatabaseError, Unit]
}
