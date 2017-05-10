package uk.ac.ncl.openlab.intake24.api.client

import uk.ac.ncl.openlab.intake24.api.client.foods.FoodsAdminClientImpl
import uk.ac.ncl.openlab.intake24.api.shared.EmailCredentials

class Intake24ApiClient(val apiBaseUrl: String, credentials: EmailCredentials) {
  val authCache = new AuthCacheImpl(apiBaseUrl, credentials)

  val foods = new FoodsAdminClientImpl(apiBaseUrl, authCache)

}
