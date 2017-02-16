package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.UserFoodData
import uk.ac.ncl.openlab.intake24.errors.LocalLookupError

trait FoodDataService {
  def getFoodData(code: String, locale: String): Either[LocalLookupError, (UserFoodData, FoodDataSources)]
}
