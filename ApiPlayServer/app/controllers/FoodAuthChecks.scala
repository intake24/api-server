package controllers

import javax.inject.Inject

import security.Intake24AccessToken
import uk.ac.ncl.openlab.intake24.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles


class FoodAuthChecks @Inject()(service: FoodsAdminService) {

  def isSuperUser(subject: Intake24AccessToken) = subject.roles.contains(Roles.superuser)

  def isFoodsAdmin(subject: Intake24AccessToken) = subject.roles.exists(r => r == Roles.superuser || r == Roles.foodsAdmin)

  def isSurveyAdmin(subject: Intake24AccessToken) = subject.roles.contains(Roles.surveyAdmin)

  def isImagesAdmin(subject: Intake24AccessToken) = subject.roles.contains(Roles.imagesAdmin)

  def isLocaleMaintainer(localeId: String, subject: Intake24AccessToken) = subject.roles.contains(Roles.foodDatabaseMaintainer(localeId))

  def isAnyLocaleMaintainer(subject: Intake24AccessToken) = subject.roles.exists(r => r.startsWith(Roles.foodDatabaseMaintainerPrefix))


  def allowAnyLocaleMaintainers(subject: Intake24AccessToken) =
    isFoodsAdmin(subject) || isAnyLocaleMaintainer(subject)

  def isMaintainerForAllLocales(locales: Seq[String], subject: Intake24AccessToken) =
    locales.nonEmpty && locales.forall(localeId => isLocaleMaintainer(localeId, subject))


  def allowLocaleMaintainers(locale: String, subject: Intake24AccessToken) =
    isFoodsAdmin(subject) || isLocaleMaintainer(locale, subject)

  def allowAdmins(subject: Intake24AccessToken) =
    isFoodsAdmin(subject)

  def allowAnyStaff(subject: Intake24AccessToken) =
    isSurveyAdmin(subject) || isFoodsAdmin(subject) || isAnyLocaleMaintainer(subject) || subject.roles.exists(r => r.endsWith(Roles.staffSuffix))

  def canReadLocales(subject: Intake24AccessToken) = allowAnyStaff(subject)


  def canCheckFoodCodes(subject: Intake24AccessToken) = allowAnyLocaleMaintainers(subject)

  def canReadFoods(locale: String)(subject: Intake24AccessToken) = allowLocaleMaintainers(locale, subject)


  def canCreateMainFoods(subject: Intake24AccessToken) = allowAdmins(subject)

  def canUpdateMainFood(foodCode: String)(subject: Intake24AccessToken): Either[DatabaseError, Boolean] = {

    val isAdmin = isFoodsAdmin(subject)
    val isMaintainer = isAnyLocaleMaintainer(subject)

    // Reject non-admins early
    if (!isAdmin && !isMaintainer)
      Right(false)
    // Allow admins to update any food without database check
    else if (isAdmin)
      Right(true)
    else
      service.getFoodLocaleRestrictions(foodCode).right.map {
        restrictions =>
          isMaintainerForAllLocales(restrictions, subject)
      }
  }

  def canUpdateCategories(subject: Intake24AccessToken) = isFoodsAdmin(subject)

  def canDeleteCategories(subject: Intake24AccessToken) = isFoodsAdmin(subject)

  def canDeleteFood(foodCode: String)(subject: Intake24AccessToken) = canUpdateMainFood(foodCode)(subject)


  def canCreateLocalFoods(locale: String)(subject: Intake24AccessToken) = allowLocaleMaintainers(locale, subject)

  def canUpdateLocalFoods(locale: String)(subject: Intake24AccessToken) = allowLocaleMaintainers(locale, subject)


  def canReadFoodGroups(subject: Intake24AccessToken) = allowAnyLocaleMaintainers(subject)

  def canUpdateFoodGroups(subject: Intake24AccessToken) = allowAdmins(subject)


  def canReadNutrientTables(subject: Intake24AccessToken) = allowAnyLocaleMaintainers(subject)

  def canReadPortionSizeMethods(subject: Intake24AccessToken) = allowAnyLocaleMaintainers(subject) || isImagesAdmin(subject)

  def canWritePortionSizeMethods(subject: Intake24AccessToken) = isSuperUser(subject) || isImagesAdmin(subject)

  def canUploadSourceImages(subject: Intake24AccessToken) = isSuperUser(subject) || isImagesAdmin(subject)

  def canDeleteSourceImages(subject: Intake24AccessToken) = isSuperUser(subject) || isImagesAdmin(subject)
}
