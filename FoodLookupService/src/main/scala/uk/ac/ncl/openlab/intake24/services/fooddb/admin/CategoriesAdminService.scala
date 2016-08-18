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

trait CategoriesAdminService {
  def getCategoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord]
  
  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean]
  def isCategoryCode(code: String): Either[DatabaseError, Boolean]
  
  def createCategory(newCategory: NewCategory): Either[CreateError, Unit]
  def createCategories(newCategories: Seq[NewCategory]): Either[CreateError, Unit]
  def createLocalCategories(localCategoryRecords: Map[String, LocalCategoryRecord], locale: String): Either[CreateError, Unit]
  
  def deleteAllCategories(): Either[DatabaseError, Unit]
  def deleteCategory(categoryCode: String): Either[DeleteError, Unit]

  def updateCategoryMainRecord(categoryCode: String, categoryMain: MainCategoryRecord): Either[UpdateError, Unit]
  def updateCategoryLocalRecord(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[LocalUpdateError, Unit]
    
  def addFoodToCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]
  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit]
  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]
  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]  
}
