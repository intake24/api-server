package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleError

trait QuickSearchService {
  def searchFoods(searchTerm: String, locale: String): Either[LocaleError, Seq[FoodHeader]]

  def searchCategories(searchTerm: String, locale: String): Either[LocaleError, Seq[CategoryHeader]]
}
