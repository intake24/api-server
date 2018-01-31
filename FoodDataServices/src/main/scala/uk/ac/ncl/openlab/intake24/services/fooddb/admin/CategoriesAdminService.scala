package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.api.data.admin._
import uk.ac.ncl.openlab.intake24.errors._

trait CategoriesAdminService {

  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord]

  def isCategoryCodeAvailable(code: String): Either[UnexpectedDatabaseError, Boolean]

  def isCategoryCode(code: String): Either[UnexpectedDatabaseError, Boolean]

  def deleteCategory(categoryCode: String): Either[DeleteError, Unit]

  def deleteAllCategories(): Either[UnexpectedDatabaseError, Unit]

  def createMainCategoryRecords(records: Seq[NewMainCategoryRecord]): Either[DependentCreateError, Unit]

  def createLocalCategoryRecords(localCategoryRecords: Map[String, NewLocalCategoryRecord], locale: String): Either[LocalCreateError, Unit]

  def updateMainCategoryRecord(categoryCode: String, mainCategoryUpdate: MainCategoryRecordUpdate): Either[DependentUpdateError, Unit]

  def updateLocalCategoryRecord(categoryCode: String, localCategoryUpdate: LocalCategoryRecordUpdate, locale: String): Either[LocalDependentUpdateError, Unit]
}
