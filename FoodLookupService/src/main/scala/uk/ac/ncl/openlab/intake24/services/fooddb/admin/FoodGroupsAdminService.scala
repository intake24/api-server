package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError

trait FoodGroupsAdminService {
  def listFoodGroups(locale: String): Either[LocaleError, Map[Int, FoodGroupRecord]]

  def getFoodGroup(code: Int, locale: String): Either[LocalLookupError, FoodGroupRecord]

  def deleteAllFoodGroups(): Either[DatabaseError, Unit]

  def createFoodGroups(foodGroups: Seq[FoodGroupMain]): Either[DatabaseError, Unit]

  def deleteLocalFoodGroups(locale: String): Either[DatabaseError, Unit]

  def createLocalFoodGroups(localFoodGroups: Map[Int, String], locale: String): Either[DatabaseError, Unit]  
}
