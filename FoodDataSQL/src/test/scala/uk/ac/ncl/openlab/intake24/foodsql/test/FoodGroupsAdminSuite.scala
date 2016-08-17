package uk.ac.ncl.openlab.intake24.foodsql.test

import org.scalatest.FunSuite
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import org.scalatest.DoNotDiscover
import uk.ac.ncl.openlab.intake24.AsServedImage
import uk.ac.ncl.openlab.intake24.AsServedSet

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale

@DoNotDiscover
class FoodGroupsAdminSuite(service: FoodGroupsAdminService) extends FunSuite {
  
  /* 
   *   def listFoodGroups(locale: String): Either[LocaleError, Map[Int, FoodGroupRecord]]

  def getFoodGroup(code: Int, locale: String): Either[LocalLookupError, FoodGroupRecord]

  def deleteAllFoodGroups(): Either[DatabaseError, Unit]

  def createFoodGroups(foodGroups: Seq[FoodGroupMain]): Either[DatabaseError, Unit]

  def deleteLocalFoodGroups(locale: String): Either[DatabaseError, Unit]

  def createLocalFoodGroups(localFoodGroups: Map[Int, String], locale: String): Either[DatabaseError, Unit]  
   */
  
   test ("list food groups") {
     assert(service.listFoodGroups("hurrdurr") === Left(UndefinedLocale))
   }
}