package security

object Roles {
  val superuser = "superuser"
  val survey_respondent = "respondent"
  val survey_admin = "admin"

  def surveyStaff(surveyId: String) = s"$surveyId/staff"

  def surveyRespondent(surveyId: String) = s"$surveyId/respondent"
}
