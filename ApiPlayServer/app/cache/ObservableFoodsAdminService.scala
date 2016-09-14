package cache

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import uk.ac.ncl.openlab.intake24.CategoryRecord
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import com.google.inject.Inject
import com.google.inject.Singleton
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentCreateError
import uk.ac.ncl.openlab.intake24.NewLocalFoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.LocalFoodRecordUpdate
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import com.google.inject.matcher.Matchers.AnnotatedWith

import modules.BasicImpl

trait FoodsAdminObserver {
  def onMainFoodRecordUpdated(code: String): Unit
  def onLocalFoodRecordCreated(code: String, locale: String): Unit
  def onLocalFoodRecordUpdated(code: String, locale: String): Unit
  def onFoodDeleted(code: String): Unit
  def onFoodCreated(code: String): Unit
  def onAllFoodsDeleted(): Unit
}

trait ObservableFoodsAdminService extends FoodsAdminService {
  def addObserver(observer: FoodsAdminObserver): Unit
}

@Singleton
class ObservableFoodsAdminServiceImpl @Inject() (@BasicImpl service: FoodsAdminService) extends ObservableFoodsAdminService {

  private var observers = List[FoodsAdminObserver]()

  def addObserver(observer: FoodsAdminObserver) = {
    observers ::= observer
  }

  def getFoodRecord(code: String, locale: String): Either[LocalLookupError, FoodRecord] = service.getFoodRecord(code, locale)

  def isFoodCodeAvailable(code: String): Either[DatabaseError, Boolean] = service.isFoodCodeAvailable(code)
  def isFoodCode(code: String): Either[DatabaseError, Boolean] = service.isFoodCode(code)

  def createFood(newFood: NewFood): Either[DependentCreateError, Unit] = service.createFood(newFood).right.map {
    _ => observers.foreach(_.onFoodCreated(newFood.code))
  }

  def createFoodWithTempCode(newFood: NewFood): Either[DependentCreateError, String] = service.createFoodWithTempCode(newFood).right.map {
    newCode =>
      observers.foreach(_.onFoodCreated(newCode))
      newCode
  }

  def createFoods(newFoods: Seq[NewFood]): Either[DependentCreateError, Unit] = service.createFoods(newFoods).right.map {
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

  def updateMainFoodRecord(foodCode: String, foodBase: MainFoodRecordUpdate): Either[LocalDependentUpdateError, Unit] = service.updateMainFoodRecord(foodCode, foodBase).right.map {
    _ =>
      observers.foreach(_.onMainFoodRecordUpdated(foodCode))
  }

  def updateLocalFoodRecord(foodCode: String, foodLocal: LocalFoodRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit] = service.updateLocalFoodRecord(foodCode, foodLocal, locale).right.map {
    _ =>
      observers.foreach(_.onLocalFoodRecordUpdated(foodCode, locale))
  }

  def deleteAllFoods(): Either[DatabaseError, Unit] = service.deleteAllFoods().right.map {
    _ =>
      observers.foreach(_.onAllFoodsDeleted())
  }

  def deleteFoods(foodCodes: Seq[String]): Either[DeleteError, Unit] = service.deleteFoods(foodCodes).right.map {
    _ =>
      foodCodes.foreach {
        foodCode =>
          observers.foreach(_.onFoodDeleted(foodCode))
      }
  }

}