package cache

import play.api.cache.CacheApi

trait FoodBrowsingCacheKeys {
  
  val cache: CacheApi
  
  def rootCategoriesCacheKey(locale: String) = s"CachedFoodBrowsingAdminDataService.rootCategories.$locale"

  def invalidateRootCategories(locale: String) = cache.remove(rootCategoriesCacheKey(locale))
}
