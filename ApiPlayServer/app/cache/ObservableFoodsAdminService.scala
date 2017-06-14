package cache

import com.google.inject.{Inject, Singleton}
import modules.BasicImpl
import uk.ac.ncl.openlab.intake24._
import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService

trait FoodsAdminObserver {
  def onMainFoodRecordUpdated(originalCode: String, newCode: String): Unit

  def onLocalFoodRecordCreated(code: String, locale: String): Unit

  def onLocalFoodRecordUpdated(code: String, locale: String): Unit

  def onFoodToBeDeleted(code: String): Unit

  def onFoodDeleted(code: String): Unit

  def onFoodCreated(code: String): Unit

  def onAllFoodsDeleted(): Unit
}

trait ObservableFoodsAdminService extends FoodsAdminService {
  def addObserver(observer: FoodsAdminObserver): Unit
}

@Singleton
class ObservableFoodsAdminServiceImpl @Inject()(@BasicImpl service: FoodsAdminService) extends ObservableFoodsAdminService {

  private var observers = List[FoodsAdminObserver]()

  def addObserver(observer: FoodsAdminObserver) = {
    observers ::= observer
  }

  def getFoodRecord(code: String, locale: String): Either[LocalLookupError, FoodRecord] = service.getFoodRecord(code, locale)

  def getFoodLocaleRestrictions(code: String): Either[LookupError, Seq[String]] = service.getFoodLocaleRestrictions(code)

  def isFoodCodeAvailable(code: String): Either[UnexpectedDatabaseError, Boolean] = service.isFoodCodeAvailable(code)

  def isFoodCode(code: String): Either[UnexpectedDatabaseError, Boolean] = service.isFoodCode(code)

  def createFood(newFood: NewMainFoodRecord): Either[DependentCreateError, Unit] = service.createFood(newFood).right.map {
    _ => observers.foreach(_.onFoodCreated(newFood.code))
  }

  def createFoodWithTempCode(newFood: NewMainFoodRecord): Either[DependentCreateError, String] = service.createFoodWithTempCode(newFood).right.map {
    newCode =>
      observers.foreach(_.onFoodCreated(newCode))
      newCode
  }

  def createFoods(newFoods: Seq[NewMainFoodRecord]): Either[DependentCreateError, Unit] = service.createFoods(newFoods).right.map {
    _ =>
      newFoods.foreach {
        food =>
          observers.foreach(_.onFoodCreated(food.code))
      }
  }

  def createLocalFoodRecords(localFoodRecords: Map[String, NewLocalFoodRecord], locale: String): Either[LocalDependentCreateError, Unit] = service.createLocalFoodRecords(localFoodRecords, locale).right.map {
    _ =>
      localFoodRecords.keySet.foreach {
        code =>
          observers.foreach(_.onLocalFoodRecordCreated(code, locale))
      }
  }

  def updateMainFoodRecord(foodCode: String, update: MainFoodRecordUpdate): Either[LocalDependentUpdateError, Unit] = service.updateMainFoodRecord(foodCode, update).right.map {
    _ =>
      observers.foreach(_.onMainFoodRecordUpdated(foodCode, update.code))
  }

  def updateLocalFoodRecord(foodCode: String, foodLocal: LocalFoodRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit] = service.updateLocalFoodRecord(foodCode, foodLocal, locale).right.map {
    _ =>
      observers.foreach(_.onLocalFoodRecordUpdated(foodCode, locale))
  }

  def deleteAllFoods(): Either[UnexpectedDatabaseError, Unit] = service.deleteAllFoods().right.map {
    _ =>
      observers.foreach(_.onAllFoodsDeleted())
  }

  def deleteFoods(foodCodes: Seq[String]): Either[DeleteError, Unit] = {
    foodCodes.foreach {
      foodCode =>
        observers.foreach(_.onFoodToBeDeleted(foodCode))
    }

    service.deleteFoods(foodCodes).right.map {
      _ =>
        foodCodes.foreach {
          foodCode =>
            observers.foreach(_.onFoodDeleted(foodCode))
        }
    }
  }
}
