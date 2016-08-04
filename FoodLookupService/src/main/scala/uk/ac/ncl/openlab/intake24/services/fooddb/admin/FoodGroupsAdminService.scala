package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.FoodGroupRecord

trait FoodGroupsAdminService {
  def allFoodGroups(locale: String): Seq[FoodGroupRecord]

  def foodGroup(code: Int, locale: String): Option[FoodGroupRecord]

  def deleteAllFoodGroups()

  def createFoodGroups(foodGroups: Seq[FoodGroupMain])

  def deleteLocalFoodGroups(locale: String)

  def createLocalFoodGroups(localFoodGroups: Map[Int, String], locale: String)  
}
