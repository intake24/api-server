package uk.ac.ncl.openlab.intake24.api.client.foods

import uk.ac.ncl.openlab.intake24.{FoodRecord, LocalFoodRecordUpdate}
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.HttpRequestUtil
import uk.ac.ncl.openlab.intake24.api.client.{ApiError, ApiResponseParser, AuthCache}
import io.circe.generic.auto._

class FoodsAdminClientImpl(apiBaseUrl: String, authCache: AuthCache) extends FoodsAdminClient with HttpRequestUtil with ApiResponseParser {

  override def getFoodRecord(foodCode: String, locale: String): Either[ApiError, FoodRecord] = authCache.withAccessToken {
    authToken =>
      parseApiResponse[FoodRecord](getAuthGetRequestNoBody(s"$apiBaseUrl/admin/foods/$locale/$foodCode", authToken).asString)
  }

  override def updateLocalFoodRecord(foodCode: String, locale: String, update: LocalFoodRecordUpdate): Either[ApiError, Unit] = authCache.withAccessToken {
    authToken =>
      parseApiResponseDiscardBody(getAuthPostRequest(s"$apiBaseUrl/admin/foods/$locale/$foodCode", authToken, update).asString)
  }

}
