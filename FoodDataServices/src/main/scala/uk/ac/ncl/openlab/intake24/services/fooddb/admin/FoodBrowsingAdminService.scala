package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.CategoryContents
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError

trait FoodBrowsingAdminService {
  
  def getUncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]]

  def getRootCategories(locale: String): Either[LocaleError, Seq[CategoryHeader]]

  def getCategoryContents(code: String, locale: String): Either[LocalLookupError, CategoryContents]

  def getFoodParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]

  def getFoodAllCategoriesCodes(code: String): Either[LookupError, Set[String]]

  def getFoodAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
  
  def getCategoryParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
  
  def getCategoryAllCategoriesCodes(code: String): Either[LookupError, Set[String]]
  
  def getCategoryAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
}
