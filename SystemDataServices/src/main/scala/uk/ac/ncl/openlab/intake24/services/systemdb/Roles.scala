package uk.ac.ncl.openlab.intake24.services.systemdb

object Roles {
  val superuser = "superuser"

  val globalSupport = "globalsupport"

  val surveyAdmin = "surveyadmin"

  val foodsAdmin = "foodsadmin"

  val respondentSuffix = "/respondent"

  val staffSuffix = "/staff"

  def surveyStaff(surveyId: String) = s"$surveyId$staffSuffix"

  def surveySupport(surveyId: String) = s"$surveyId/support"

  def surveyRespondent(surveyId: String) = s"$surveyId$respondentSuffix"

  def foodDatabaseMaintainer(localeId: String) = s"fdbm/$localeId"



}
