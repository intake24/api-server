package uk.ac.ncl.openlab.intake24.api.client.foods

import uk.ac.ncl.openlab.intake24.{FoodRecord, LocalFoodRecordUpdate}
import uk.ac.ncl.openlab.intake24.api.client.ApiError

trait FoodsAdminClient {

  def getFoodRecord(foodCode: String, locale: String): Either[ApiError, FoodRecord]
  def updateLocalFoodRecord(foodCode: String, locale: String, update: LocalFoodRecordUpdate): Either[ApiError, Unit]
}
