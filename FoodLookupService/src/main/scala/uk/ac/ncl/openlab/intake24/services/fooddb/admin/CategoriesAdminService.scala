package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

import uk.ac.ncl.openlab.intake24.CategoryRecord
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DeleteError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentError
import uk.ac.ncl.openlab.intake24.MainCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentUpdateError
import uk.ac.ncl.openlab.intake24.LocalCategoryRecordUpdate
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.NewLocalCategoryRecord
import uk.ac.ncl.openlab.intake24.NewMainCategoryRecord
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DependentCreateError

trait CategoriesAdminService {
  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord]

  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean]
  def isCategoryCode(code: String): Either[DatabaseError, Boolean]

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit]

  def deleteAllCategories(): Either[DatabaseError, Unit]

  def createMainCategoryRecords(records: Seq[NewMainCategoryRecord]): Either[DependentCreateError, Unit]
  def createLocalCategoryRecords(localCategoryRecords: Map[String, NewLocalCategoryRecord], locale: String): Either[CreateError, Unit]

  def updateMainCategoryRecord(categoryCode: String, mainCategoryUpdate: MainCategoryRecordUpdate): Either[DependentUpdateError, Unit]
  def updateLocalCategoryRecord(categoryCode: String, localCategoryUpdate: LocalCategoryRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit]
}
