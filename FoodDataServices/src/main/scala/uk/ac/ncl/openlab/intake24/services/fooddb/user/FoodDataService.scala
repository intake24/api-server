package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.errors.LocalLookupError

case class UserFoodData(code: String, localDescription: String, groupCode: Int, readyMealOption: Boolean, sameAsBeforeOption: Boolean, nutrientTableCodes: Map[String, String], portionSizeMethods: Seq[PortionSizeMethod])

trait FoodDataService {
  def getFoodData(code: String, locale: String): Either[LocalLookupError, (UserFoodData, FoodDataSources)]
}
