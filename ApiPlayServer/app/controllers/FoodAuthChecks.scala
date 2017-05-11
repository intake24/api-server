package controllers

import security.Intake24AccessToken
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles

class FoodAuthChecks {

  def allowAnyLocaleMaintainers(subject: Intake24AccessToken) =
    subject.roles.exists(r => r == Roles.superuser || r == Roles.foodsAdmin || r.startsWith(Roles.foodDatabaseMaintainerPrefix))

  def allowLocaleMaintainers(locale: String, subject: Intake24AccessToken) =
    subject.roles.exists(r => r == Roles.superuser || r == Roles.foodsAdmin || r == Roles.foodDatabaseMaintainer(locale))

  def allowAdmins(subject: Intake24AccessToken) =
    subject.roles.exists(r => r == Roles.superuser || r == Roles.foodsAdmin)

  def allowAnyStaff(subject: Intake24AccessToken) =
    subject.roles.exists(r => r == Roles.superuser || r == Roles.foodsAdmin || r.startsWith(Roles.foodDatabaseMaintainerPrefix) || r.endsWith(Roles.staffSuffix))


  def canReadLocales(subject: Intake24AccessToken) = allowAnyStaff(subject)


  def canCheckFoodCodes(subject: Intake24AccessToken) = allowAnyLocaleMaintainers(subject)

  def canReadFoods(locale: String)(subject: Intake24AccessToken) = allowLocaleMaintainers(locale, subject)


  def canCreateMainFoods(subject: Intake24AccessToken) = allowAdmins(subject)

  def canUpdateMainFoods(subject: Intake24AccessToken) = allowAdmins(subject)

  def canDeleteFoods(subject: Intake24AccessToken) = allowAdmins(subject)


  def canCreateLocalFoods(locale: String)(subject: Intake24AccessToken) = allowLocaleMaintainers(locale, subject)

  def canUpdateLocalFoods(locale: String)(subject: Intake24AccessToken) = allowLocaleMaintainers(locale, subject)


  def canReadFoodGroups(subject: Intake24AccessToken) = allowAnyLocaleMaintainers(subject)

  def canUpdateFoodGroups(subject: Intake24AccessToken) = allowAdmins(subject)


  def canReadNutrientTables(subject: Intake24AccessToken) = allowAnyLocaleMaintainers(subject)

  def canReadPortionSizeMethods(subject: Intake24AccessToken) = allowAnyLocaleMaintainers(subject)

  def canWritePortionSizeMethods(subject: Intake24AccessToken) = allowAdmins(subject)


  def canUploadSourceImages(subject: Intake24AccessToken) = allowAdmins(subject)


}
