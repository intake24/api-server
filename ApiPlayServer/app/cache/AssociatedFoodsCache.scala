package cache

import play.api.cache.CacheApi

trait AssociatedFoodsCache {
  
  val cache: CacheApi
  
  def associatedFoodsCacheKey(code: String, locale: String) = s"AssociatedFoods.$locale.$code"
  
  def invalidateAssociatedFoods(code: String, locale: String) = cache.remove(associatedFoodsCacheKey(code, locale))
}