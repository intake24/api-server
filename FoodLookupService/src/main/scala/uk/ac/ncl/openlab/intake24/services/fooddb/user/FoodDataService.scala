package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.UserFoodData
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.FoodDataError

trait FoodDataService {
  def foodData(code: String, locale: String): Either[FoodDataError, (UserFoodData, FoodDataSources)]
}
