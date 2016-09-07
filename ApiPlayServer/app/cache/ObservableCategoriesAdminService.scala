package cache

import uk.ac.ncl.openlab.intake24.services.fooddb.admin.CategoriesAdminService
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
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError

trait CategoriesAdminObserver {
  def onMainCategoryRecordUpdated(code: String): Unit
  def onLocalCategoryRecordUpdated(code: String, locale: String): Unit
  def onCategoryDeleted(code: String): Unit
  def onCategoryCreated(code: String): Unit
  def onLocalCategoryRecordCreated(code: String, locale: String): Unit
  def onAllCategoriesDeleted(): Unit
  def onFoodAddedToCategory(categoryCode: String, foodCode: String): Unit
  def onFoodRemovedFromCategory(categoryCode: String, foodCode: String): Unit
  def onSubcategoryAddedToCategory(categoryCode: String, subcategoryCode: String): Unit
  def onSubcategoryRemovedFromCategory(categoryCode: String, subcategoryCode: String): Unit
}

trait ObservableCategoriesAdminService extends CategoriesAdminService {
  def addObserver(observer: CategoriesAdminObserver): Unit
}

class ObservableCategoriesAdminServiceImpl @Inject() (service: CategoriesAdminService) extends ObservableCategoriesAdminService {

  private var observers = List[CategoriesAdminObserver]()
  
  def addObserver(observer: CategoriesAdminObserver) = observers ::= observer

  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord] = service.getCategoryRecord(code, locale)

  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean] = service.isCategoryCodeAvailable(code)
  def isCategoryCode(code: String): Either[DatabaseError, Boolean] = service.isCategoryCode(code)

  def createCategory(newCategory: NewCategory): Either[CreateError, Unit] = service.createCategory(newCategory).right.map {
    _ => observers.foreach(_.onCategoryCreated(newCategory.code))
  }

  def createCategories(newCategories: Seq[NewCategory]): Either[CreateError, Unit] = service.createCategories(newCategories).right.map {
    _ =>
      newCategories.foreach {
        nc => observers.foreach(_.onCategoryCreated(nc.code))
      }
  }

  def createLocalCategories(localCategoryRecords: Map[String, LocalCategoryRecord], locale: String): Either[CreateError, Unit] =
    service.createLocalCategories(localCategoryRecords, locale).right.map {
      _ =>
        localCategoryRecords.keySet.foreach {
          code => observers.foreach(_.onLocalCategoryRecordCreated(code, locale))
        }
    }

  def deleteAllCategories(): Either[DatabaseError, Unit] = service.deleteAllCategories().right.map {
    _ => observers.foreach(_.onAllCategoriesDeleted())
  }

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit] = service.deleteCategory(categoryCode).right.map {
    _ => observers.foreach(_.onCategoryDeleted(categoryCode))
  }

  def updateMainCategoryRecord(categoryCode: String, categoryMain: MainCategoryRecord): Either[UpdateError, Unit] =
    service.updateMainCategoryRecord(categoryCode, categoryMain).right.map {
      _ => observers.foreach(_.onMainCategoryRecordUpdated(categoryCode))
    }

  def updateLocalCategoryRecord(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[LocalUpdateError, Unit] =
    service.updateLocalCategoryRecord(categoryCode, locale, categoryLocal).right.map {
      _ => observers.foreach(_.onLocalCategoryRecordUpdated(categoryCode, locale))
    }

  def addFoodToCategory(categoryCode: String, foodCode: String): Either[ParentError, Unit] =
    service.addFoodToCategory(categoryCode, foodCode).right.map {
      _ => observers.foreach(_.onFoodAddedToCategory(categoryCode, foodCode))
    }

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[ParentError, Unit] = service.addSubcategoryToCategory(categoryCode, subcategoryCode).right.map {
    _ => observers.foreach(_.onSubcategoryAddedToCategory(categoryCode, subcategoryCode))
  }

  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[LookupError, Unit] = service.removeFoodFromCategory(categoryCode, foodCode).right.map {
    _ => observers.foreach(_.onFoodRemovedFromCategory(categoryCode, foodCode))
  }

  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): Either[LookupError, Unit] = service.removeSubcategoryFromCategory(categoryCode, foodCode).right.map {
    _ => observers.foreach(_.onSubcategoryRemovedFromCategory(categoryCode, foodCode))
  }

}