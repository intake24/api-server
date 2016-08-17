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
  
  def uncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]]

  def rootCategories(locale: String): Either[LocaleError, Seq[CategoryHeader]]

  def categoryContents(code: String, locale: String): Either[LocalLookupError, CategoryContents]

  def foodParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]

  def foodAllCategoriesCodes(code: String): Either[LookupError, Seq[String]]

  def foodAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
  
  def categoryParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
  
  def categoryAllCategoriesCodes(code: String): Either[LookupError, Seq[String]]
  
  def categoryAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
}
