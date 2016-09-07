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

trait CategoriesAdminService {
  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord]
  
  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean]
  def isCategoryCode(code: String): Either[DatabaseError, Boolean]
  
  def createCategory(newCategory: NewCategory): Either[CreateError, Unit]
  def createCategories(newCategories: Seq[NewCategory]): Either[CreateError, Unit]
  def createLocalCategories(localCategoryRecords: Map[String, NewLocalCategoryRecord], locale: String): Either[CreateError, Unit]
  
  def deleteAllCategories(): Either[DatabaseError, Unit]
  def deleteCategory(categoryCode: String): Either[DeleteError, Unit]

  def updateMainCategoryRecord(categoryCode: String, mainCategoryUpdate: MainCategoryRecordUpdate): Either[DependentUpdateError, Unit]
  def updateLocalCategoryRecord(categoryCode: String, localCategoryUpdate: LocalCategoryRecordUpdate, locale: String): Either[LocalUpdateError, Unit]
    
  def addFoodToCategory(categoryCode: String, foodCode: String): Either[ParentError, Unit]
  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[ParentError, Unit]
  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[LookupError, Unit]
  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): Either[LookupError, Unit]  
}
