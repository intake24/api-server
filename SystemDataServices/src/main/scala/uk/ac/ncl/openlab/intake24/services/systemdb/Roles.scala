package uk.ac.ncl.openlab.intake24.services.systemdb

object Roles {
  val superuser = "superuser"

  val globalsupport = "globalsupport"

  def surveyStaff(surveyId: String) = s"$surveyId/staff"

  def surveySupport(surveyId: String) = s"$surveyId/support"

  def surveyRespondent(surveyId: String) = s"$surveyId/respondent"
}
