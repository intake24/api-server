package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.api.data.admin._
import uk.ac.ncl.openlab.intake24.errors._

trait FoodsAdminService {

  def getFoodRecord(code: String, locale: String): Either[LocalLookupError, FoodRecord]

  def getFoodLocaleRestrictions(code: String): Either[LookupError, Seq[String]]

  def isFoodCodeAvailable(code: String): Either[UnexpectedDatabaseError, Boolean]

  def isFoodCode(code: String): Either[UnexpectedDatabaseError, Boolean]

  def createFood(newFood: NewMainFoodRecord): Either[DependentCreateError, Unit]

  def createFoodWithTempCode(newFood: NewMainFoodRecord): Either[DependentCreateError, String]

  def createFoods(newFoods: Seq[NewMainFoodRecord]): Either[DependentCreateError, Unit]

  def createLocalFoodRecords(localFoodRecords: Map[String, NewLocalFoodRecord], locale: String): Either[LocalDependentCreateError, Unit]

  def updateMainFoodRecord(foodCode: String, update: MainFoodRecordUpdate): Either[LocalDependentUpdateError, Unit]

  def updateLocalFoodRecord(foodCode: String, update: LocalFoodRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit]

  def deleteAllFoods(): Either[UnexpectedDatabaseError, Unit]

  def deleteFoods(foodCodes: Seq[String]): Either[DeleteError, Unit]
}
