package uk.ac.ncl.openlab.intake24.services.systemdb

object Roles {
  val superuser = "superuser"

  def surveyStaff(surveyId: String) = s"$surveyId/staff"

  def surveyRespondent(surveyId: String) = s"$surveyId/respondent"
}
