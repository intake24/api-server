package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.api.data.admin.{CategoryHeader, FoodHeader}
import uk.ac.ncl.openlab.intake24.errors.LocaleError


trait QuickSearchService {
  def searchFoods(searchTerm: String, locale: String): Either[LocaleError, Seq[FoodHeader]]

  def searchCategories(searchTerm: String, locale: String): Either[LocaleError, Seq[CategoryHeader]]
}
