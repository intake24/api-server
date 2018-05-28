package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.api.data.{PortionSizeMethod, UserFoodHeader}
import uk.ac.ncl.openlab.intake24.errors.LocalLookupError

case class ResolvedFoodData(code: String, englishDescription: String, localDescription: String, groupCode: Int,
                            reasonableAmount: Int, readyMealOption: Boolean, sameAsBeforeOption: Boolean,
                            nutrientTableCodes: Map[String, String], portionSizeMethods: Seq[PortionSizeMethod])

trait FoodDataService {
  def getFoodData(code: String, locale: String): Either[LocalLookupError, (ResolvedFoodData, FoodDataSources)]
  def getFoodHeader(code: String, localeId: String): Either[LocalLookupError, UserFoodHeader]
}
