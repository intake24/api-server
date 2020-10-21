package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.time.{Instant, ZonedDateTime}

import uk.ac.ncl.openlab.intake24.errors._

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

case class CustomFieldDescription(key: String, description: String)

case class CustomDataScheme(userCustomFields: Seq[CustomFieldDescription], surveyCustomFields: Seq[CustomFieldDescription],
                            mealCustomFields: Seq[CustomFieldDescription], foodCustomFields: Seq[CustomFieldDescription])

case class SurveyParametersIn(id: String, schemeId: String, localeId: String, state: Int,
                              startDate: ZonedDateTime, endDate: ZonedDateTime,
                              allowGeneratedUsers: Boolean, generateUserKey: Option[String],
                              externalFollowUpURL: Option[String], supportEmail: String,
                              description: Option[String], finalPageHtml: Option[String],
                              submissionNotificationUrl: Option[String],
                              feedbackEnabled: Boolean, numberOfSubmissionsForFeedback: Int,
                              storeUserSessionOnServer: Option[Boolean],
                              maximumDailySubmissions: Int,
                              maximumTotalSubmissions: Option[Int],
                              minimumSubmissionInterval: Int)

case class SurveyParametersOut(id: String, schemeId: String, localeId: String, state: Int,
                               startDate: ZonedDateTime, endDate: ZonedDateTime,
                               suspensionReason: Option[String], allowGeneratedUsers: Boolean,
                               generateUserKey: Option[String],
                               externalFollowUpURL: Option[String], supportEmail: String,
                               description: Option[String], finalPageHtml: Option[String],
                               submissionNotificationUrl: Option[String],
                               feedbackEnabled: Boolean, numberOfSubmissionsForFeedback: Int,
                               storeUserSessionOnServer: Option[Boolean],
                               maximumDailySubmissions: Int,
                               maximumTotalSubmissions: Option[Int],
                               minimumSubmissionInterval: Int)

// Staff cannot change survey ID, scheme, locale and generated user settings
case class StaffSurveyUpdate(startDate: ZonedDateTime, endDate: ZonedDateTime,
                             state: Int, externalFollowUpURL: Option[String], supportEmail: String,
                             description: Option[String], finalPageHtml: Option[String])

case class LocalNutrientDescription(nutrientTypeId: Int, description: String, unit: String)

case class LocalFieldDescription(fieldName: String, description: String)

trait SurveyAdminService {

  def validateSurveyId(surveyId: String): Either[CreateError, Unit]

  def createSurvey(parameters: SurveyParametersIn): Either[CreateError, SurveyParametersOut]

  def updateSurvey(surveyId: String, parameters: SurveyParametersIn): Either[UpdateError, SurveyParametersOut]

  def staffUpdateSurvey(surveyId: String, update: StaffSurveyUpdate): Either[UpdateError, SurveyParametersOut]

  def getSurvey(surveyId: String): Either[LookupError, SurveyParametersOut]

  def listSurveys(): Either[UnexpectedDatabaseError, Seq[SurveyParametersOut]]

  def getSurveyParameters(surveyId: String): Either[LookupError, SurveyParametersOut]

  def getCustomDataScheme(schemeId: String): Either[LookupError, CustomDataScheme]

  def getLocalNutrientTypes(localeId: String): Either[LookupError, Seq[LocalNutrientDescription]]

  def getLocalFields(localeId: String): Either[LookupError, Seq[LocalFieldDescription]]

  def deleteSurvey(surveyId: String): Either[DeleteError, Unit]

}
