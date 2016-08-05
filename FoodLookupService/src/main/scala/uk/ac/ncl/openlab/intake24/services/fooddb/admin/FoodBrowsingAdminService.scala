package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodCodeError
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.CategoryContents
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait FoodBrowsingAdminService {
  
  def uncategorisedFoods(locale: String): Either[DatabaseError, Seq[FoodHeader]]

  def rootCategories(locale: String): Either[DatabaseError, Seq[CategoryHeader]]

  def categoryContents(code: String, locale: String): Either[DatabaseError, CategoryContents]

  def foodRecord(code: String, locale: String): Either[FoodCodeError, FoodRecord]

  def foodParentCategories(code: String, locale: String): Either[DatabaseError, Seq[CategoryHeader]]

  def foodAllCategoriesCodes(code: String): Either[DatabaseError, Seq[String]]

  def foodAllCategoriesHeaders(code: String, locale: String): Either[DatabaseError, Seq[CategoryHeader]]
  
  def categoryParentCategories(code: String, locale: String): Either[DatabaseError, Seq[CategoryHeader]]
  
  def categoryAllCategoriesCodes(code: String): Either[DatabaseError, Seq[String]]
  
  def cateogryAllCategoriesHeaders(code: String, locale: String): Either[DatabaseError, Seq[CategoryHeader]]
}
