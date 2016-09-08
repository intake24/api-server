package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import uk.ac.ncl.openlab.intake24.LocalFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.NewLocalFoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentUpdateError

trait FoodsAdminService {
  def getFoodRecord(code: String, locale: String): Either[LocalLookupError, FoodRecord]
  
  def isFoodCodeAvailable(code: String): Either[DatabaseError, Boolean]
  def isFoodCode(code: String): Either[DatabaseError, Boolean]

  def createFood(newFood: NewFood): Either[DependentCreateError, Unit]
  def createFoodWithTempCode(newFood: NewFood): Either[DependentCreateError, String]
  def createFoods(newFoods: Seq[NewFood]): Either[DependentCreateError, Unit]
  def createLocalFoodRecords(localFoodRecords: Map[String, NewLocalFoodRecord], locale: String): Either[LocalDependentCreateError, Unit]

  def updateMainFoodRecord(foodCode: String, update: MainFoodRecordUpdate): Either[DependentUpdateError, Unit]
  def updateLocalFoodRecord(foodCode: String, update: LocalFoodRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit]

  def deleteAllFoods(): Either[DatabaseError, Unit]
  def deleteFood(foodCode: String): Either[DeleteError, Unit]  
}
