package uk.ac.ncl.openlab.intake24.services.systemdb.admin

import java.time.{Instant, ZonedDateTime}

import uk.ac.ncl.openlab.intake24.errors.{CreateError, DeleteError, LookupError, UnexpectedDatabaseError}

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

case class NewSurveyParameters(schemeId: String, localeId: String, allowGeneratedUsers: Boolean, externalFollowUpURL: Option[String], supportEmail: String)

case class SurveyParametersOut(id: String, schemeId: String, localeId: String, state: Int,
                               startDate: ZonedDateTime, endDate: ZonedDateTime,
                               suspensionReason: Option[String], allowGeneratedUsers: Boolean,
                               externalFollowUpURL: Option[String], supportEmail: String)

case class LocalNutrientDescription(nutrientTypeId: Int, description: String, unit: String)

trait SurveyAdminService {
  def createSurvey(surveyId: String, parameters: NewSurveyParameters): Either[CreateError, SurveyParametersOut]

  def listSurveys(): Either[UnexpectedDatabaseError, Seq[SurveyParametersOut]]

  def getSurveyParameters(surveyId: String): Either[LookupError, SurveyParametersOut]

  def getCustomDataScheme(schemeId: String): Either[LookupError, CustomDataScheme]

  def getLocalNutrientTypes(localeId: String): Either[LookupError, Seq[LocalNutrientDescription]]

  def deleteSurvey(surveyId: String): Either[DeleteError, Unit]

}
