package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.errors.{CreateError, LocalLookupError, LocaleError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.{FoodGroupLocal, FoodGroupMain, FoodGroupRecord}

trait FoodGroupsAdminService {
  def listFoodGroups(locale: String): Either[LocaleError, Map[Int, FoodGroupRecord]]

  def getFoodGroup(code: Int, locale: String): Either[LocalLookupError, FoodGroupRecord]

  def deleteAllFoodGroups(): Either[UnexpectedDatabaseError, Unit]

  def createFoodGroup(foodGroup: FoodGroupMain): Either[CreateError, Unit]

  def createFoodGroups(foodGroups: Seq[FoodGroupMain]): Either[CreateError, Unit]

  def deleteLocalFoodGroups(locale: String): Either[UnexpectedDatabaseError, Unit]

  def createLocalFoodGroups(localFoodGroups: Map[Int, FoodGroupLocal], locale: String): Either[UnexpectedDatabaseError, Unit]
}
