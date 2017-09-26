package cache

import play.api.cache.SyncCacheApi

trait FoodBrowsingCacheKeys {
  
  val cache: SyncCacheApi
  
  def rootCategoriesCacheKey(locale: String) = s"CachedFoodBrowsingAdminDataService.rootCategories.$locale"

  def invalidateRootCategories(locale: String) = cache.remove(rootCategoriesCacheKey(locale))
}
