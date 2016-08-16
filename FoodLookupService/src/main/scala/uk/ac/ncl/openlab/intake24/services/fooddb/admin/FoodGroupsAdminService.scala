package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait FoodGroupsAdminService {
  def allFoodGroups(locale: String): Either[DatabaseError, Seq[FoodGroupRecord]]

  def foodGroup(code: Int, locale: String): Either[ResourceError, FoodGroupRecord]

  def deleteAllFoodGroups(): Either[DatabaseError, Unit]

  def createFoodGroups(foodGroups: Seq[FoodGroupMain]): Either[DatabaseError, Unit]

  def deleteLocalFoodGroups(locale: String): Either[DatabaseError, Unit]

  def createLocalFoodGroups(localFoodGroups: Map[Int, String], locale: String): Either[DatabaseError, Unit]  
}
