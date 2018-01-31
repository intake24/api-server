package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.api.data.admin.{CategoryContents, CategoryHeader, FoodHeader}
import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, LocaleError, LookupError}


case class CategoryDescendantsCodes(foods: Set[String], subcategories: Set[String])

trait FoodBrowsingAdminService {

  def getUncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]]

  def getRootCategories(locale: String): Either[LocaleError, Seq[CategoryHeader]]

  def getCategoryContents(code: String, locale: String): Either[LocalLookupError, CategoryContents]

  def getAllCategoryDescendantsCodes(code: String): Either[LookupError, CategoryDescendantsCodes]

  def getFoodParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]

  def getFoodAllCategoriesCodes(code: String): Either[LookupError, Set[String]]

  def getFoodAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]

  def getCategoryParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]

  def getCategoryAllCategoriesCodes(code: String): Either[LookupError, Set[String]]

  def getCategoryAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
}
