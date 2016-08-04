package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CodeError
import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError

trait FoodsAdminService {
  def foodRecord(code: String, locale: String): Either[CodeError, FoodRecord]
  
  def isFoodCodeAvailable(code: String): Either[DatabaseError, Boolean]
  def isFoodCode(code: String): Either[DatabaseError, Boolean]

  def createFood(newFood: NewFood): Either[CreateError, Unit]
  def createFoodWithTempCode(newFood: NewFood): Either[DatabaseError, String]
  def createFoods(newFoods: Seq[NewFood]): Either[DatabaseError, Unit]
  def createLocalFoods(localFoodRecords: Map[String, LocalFoodRecord]): Either[DatabaseError, Unit]

  def updateMainFoodRecord(foodCode: String, foodBase: MainFoodRecord): Either[UpdateError, Unit]
  def updateLocalFoodRecord(foodCode: String, locale: String, foodLocal: LocalFoodRecord): Either[UpdateError, Unit]

  def deleteAllFoods(): Either[DatabaseError, Unit]
  def deleteFood(foodCode: String): Either[CodeError, Unit]  
}
