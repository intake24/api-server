package uk.ac.ncl.openlab.intake24.services.systemdb

object Roles {
  val superuser = "superuser"

  val globalSupport = "globalsupport"

  val surveyAdmin = "surveyadmin"

  val foodsAdmin = "foodsadmin"

  val imagesAdmin = "imagesadmin"

  val adminRoles = Set(superuser, globalSupport, surveyAdmin, foodsAdmin, imagesAdmin)

  val respondentSuffix = "/respondent"

  val staffSuffix = "/staff"

  val foodDatabaseMaintainerPrefix = "fdbm/"


  def surveyStaff(surveyId: String) = s"$surveyId$staffSuffix"

  def surveySupport(surveyId: String) = s"$surveyId/support"

  def surveyRespondent(surveyId: String) = s"$surveyId$respondentSuffix"

  def foodDatabaseMaintainer(localeId: String) = s"$foodDatabaseMaintainerPrefix$localeId"

  def isAdminRole(role: String) = adminRoles.contains(role)

  def isSurveyStaff(role: String) = role.endsWith(staffSuffix) && role.length > staffSuffix.length

  def isSurveyRespondent(role: String) = role.endsWith(respondentSuffix) && role.length > respondentSuffix.length

  def isFoodDatabaseMaintainer(role: String) = role.startsWith(foodDatabaseMaintainerPrefix) && role.length > foodDatabaseMaintainerPrefix.length

  def isValidRole(role: String): Boolean = isAdminRole(role) || isSurveyStaff(role) || isSurveyRespondent(role) || isFoodDatabaseMaintainer(role)
}
