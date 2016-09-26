package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.FoodGroupLocal

trait FoodGroupsAdminService {
  def listFoodGroups(locale: String): Either[LocaleError, Map[Int, FoodGroupRecord]]

  def getFoodGroup(code: Int, locale: String): Either[LocalLookupError, FoodGroupRecord]

  def deleteAllFoodGroups(): Either[DatabaseError, Unit]
  
  def createFoodGroup(foodGroup: FoodGroupMain): Either[CreateError, Unit]

  def createFoodGroups(foodGroups: Seq[FoodGroupMain]): Either[CreateError, Unit]

  def deleteLocalFoodGroups(locale: String): Either[DatabaseError, Unit]

  def createLocalFoodGroups(localFoodGroups: Map[Int, FoodGroupLocal], locale: String): Either[DatabaseError, Unit]  
}
