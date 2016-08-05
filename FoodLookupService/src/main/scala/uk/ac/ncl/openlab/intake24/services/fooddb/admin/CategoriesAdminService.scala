package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodCodeError
import uk.ac.ncl.openlab.intake24.CategoryRecord
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.NewCategory

trait CategoriesAdminService {
  def categoryRecord(code: String, locale: String): Either[FoodCodeError, CategoryRecord]
  
  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean]
  def isCategoryCode(code: String): Either[DatabaseError, Boolean]
  
  def createCategory(newCategory: NewCategory): Either[DatabaseError, Unit]
  
  def deleteAllCategories(): Either[DatabaseError, Unit]
  def deleteCategory(categoryCode: String): Either[DatabaseError, Unit]

  def updateCategoryMainRecord(categoryCode: String, categoryMain: MainCategoryRecord): Either[UpdateError, Unit]
  def updateCategoryLocalRecord(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[UpdateError, Unit]
    
  def addFoodToCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]
  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit]
  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]
  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]  
}
