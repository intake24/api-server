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
import com.google.inject.Singleton
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.NewLocalCategoryRecord
import uk.ac.ncl.openlab.intake24.MainCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.LocalCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.NewMainCategoryRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalCreateError
import modules.BasicImpl

/*
 *   def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord]

  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean]
  def isCategoryCode(code: String): Either[DatabaseError, Boolean]

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit]

  def deleteAllCategories(): Either[DatabaseError, Unit]

  def createMainCategoryRecords(records: Seq[NewMainCategoryRecord]): Either[DependentCreateError, Unit]
  def createLocalCategoryRecords(localCategoryRecords: Map[String, NewLocalCategoryRecord], locale: String): Either[CreateError, Unit]

  def updateMainCategoryRecord(categoryCode: String, mainCategoryUpdate: MainCategoryRecordUpdate): Either[DependentUpdateError, Unit]
  def updateLocalCategoryRecord(categoryCode: String, localCategoryUpdate: LocalCategoryRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit]
 */
trait CategoriesAdminObserver {
  def onCategoryToBeDeleted(code: String): Unit
  def onCategoryDeleted(code: String): Unit
  def onAllCategoriesDeleted(): Unit

  def onMainCategoryRecordCreated(record: NewMainCategoryRecord): Unit
  def onLocalCategoryRecordCreated(code: String, record: NewLocalCategoryRecord, locale: String): Unit

  def onMainCategoryRecordUpdated(code: String, record: MainCategoryRecordUpdate): Unit
  def onLocalCategoryRecordUpdated(code: String, record: LocalCategoryRecordUpdate, locale: String): Unit
}

trait ObservableCategoriesAdminService extends CategoriesAdminService {
  def addObserver(observer: CategoriesAdminObserver): Unit
}

@Singleton
class ObservableCategoriesAdminServiceImpl @Inject() (@BasicImpl service: CategoriesAdminService) extends ObservableCategoriesAdminService {

  private var observers = List[CategoriesAdminObserver]()

  def addObserver(observer: CategoriesAdminObserver) = observers ::= observer

  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord] = service.getCategoryRecord(code, locale)

  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean] = service.isCategoryCodeAvailable(code)
  def isCategoryCode(code: String): Either[DatabaseError, Boolean] = service.isCategoryCode(code)

  def createMainCategoryRecords(records: Seq[NewMainCategoryRecord]): Either[DependentCreateError, Unit] = service.createMainCategoryRecords(records).right.map {
    _ => records.foreach(record => observers.foreach(_.onMainCategoryRecordCreated(record)))
  }

  def updateLocalCategoryRecord(categoryCode: String, localCategoryUpdate: LocalCategoryRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit] = service.updateLocalCategoryRecord(categoryCode, localCategoryUpdate, locale).right.map {
    _ => observers.foreach(_.onLocalCategoryRecordUpdated(categoryCode, localCategoryUpdate, locale))
  }

  def createLocalCategoryRecords(localCategoryRecords: Map[String, NewLocalCategoryRecord], locale: String): Either[LocalCreateError, Unit] =
    service.createLocalCategoryRecords(localCategoryRecords, locale).right.map {
      _ =>
        localCategoryRecords.map {
          case (code, record) => observers.foreach(_.onLocalCategoryRecordCreated(code, record, locale))
        }
    }

  def deleteAllCategories(): Either[DatabaseError, Unit] = service.deleteAllCategories().right.map {
    _ => observers.foreach(_.onAllCategoriesDeleted())
  }

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit] = {
    observers.foreach(_.onCategoryToBeDeleted(categoryCode))

    service.deleteCategory(categoryCode).right.map {
      _ => observers.foreach(_.onCategoryDeleted(categoryCode))
    }
  }

  def updateMainCategoryRecord(categoryCode: String, categoryMain: MainCategoryRecordUpdate): Either[DependentUpdateError, Unit] =
    service.updateMainCategoryRecord(categoryCode, categoryMain).right.map {
      _ => observers.foreach(_.onMainCategoryRecordUpdated(categoryCode, categoryMain))
    }

  def updateLocalCategoryRecord(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecordUpdate): Either[LocalDependentUpdateError, Unit] =
    service.updateLocalCategoryRecord(categoryCode, categoryLocal, locale).right.map {
      _ => observers.foreach(_.onLocalCategoryRecordUpdated(categoryCode, categoryLocal, locale))
    }
}