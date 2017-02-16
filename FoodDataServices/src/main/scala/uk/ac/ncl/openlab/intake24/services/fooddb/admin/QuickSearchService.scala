package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.errors.LocaleError
import uk.ac.ncl.openlab.intake24.{CategoryHeader, FoodHeader}

trait QuickSearchService {
  def searchFoods(searchTerm: String, locale: String): Either[LocaleError, Seq[FoodHeader]]

  def searchCategories(searchTerm: String, locale: String): Either[LocaleError, Seq[CategoryHeader]]
}
