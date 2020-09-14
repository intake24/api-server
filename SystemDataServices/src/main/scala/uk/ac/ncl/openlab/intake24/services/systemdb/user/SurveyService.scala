package uk.ac.ncl.openlab.intake24.services.systemdb.user

import java.time.ZonedDateTime
import java.util.UUID

import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.surveydata.NutrientMappedSubmission

case class PublicSurveyParameters(localeId: String, respondentLanguageId: String, supportEmail: String, originatingURL: Option[String])

case class SurveyFeedbackStyle(feedbackStyle: String)

case class UxEventsSettings(enableSearchEvents: Boolean, enableAssociatedFoodsEvents: Boolean)

case class UserSurveyParameters(id: String, schemeId: String, localeId: String, state: String, suspensionReason: Option[String],
                                description: Option[String], finalPageHtml: Option[String], uxEventsSettings: UxEventsSettings,
                                storeUserSessionOnServer: Boolean, numberOfSurveysForFeedback: Int,
                                maximumDailySubmissions: Int, minimumSubmissionInterval: Int)

case class SurveyFollowUp(followUpUrl: Option[String], showFeedback: Boolean)

trait SurveyService {

  def getPublicSurveyParameters(surveyId: String): Either[LookupError, PublicSurveyParameters]

  def getSurveyFeedbackStyle(surveyId: String): Either[LookupError, SurveyFeedbackStyle]

  def getSurveyParameters(surveyId: String): Either[LookupError, UserSurveyParameters]

  def getSurveyFollowUp(surveyId: String): Either[LookupError, SurveyFollowUp]

  def createSubmission(userId: Long, surveyId: String, submission: NutrientMappedSubmission): Either[UnexpectedDatabaseError, UUID]

  def userSubmittedWithinPeriod(surveyId: String, userId: Long, dateFrom: ZonedDateTime, dateTo: ZonedDateTime): Either[UnexpectedDatabaseError, Boolean]

  def getNumberOfSubmissionsForUser(surveyId: String, userId: Long): Either[UnexpectedDatabaseError, Int]

  def getNumberOfSubmissionsOnDay(surveyId: String, userId: Long, dayOfYear: Int, timeZone: String): Either[UnexpectedDatabaseError, Int]

  def getLastSubmissionTime(surveyId: String, userId: Long): Either[UnexpectedDatabaseError, Option[ZonedDateTime]]

}
