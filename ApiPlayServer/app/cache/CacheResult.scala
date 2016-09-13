package cache

import play.api.cache.CacheApi

trait CacheResult {
  
  val cache: CacheApi
  
  var knownCacheKeys: Set[String]

  def cachePositiveResult[E, T](key: String)(block: => Either[E, T])(implicit ev: scala.reflect.ClassTag[T]) = cache.get[T](key) match {
    case Some(cached) =>  Right(cached)
    
    case None => block match {
      case Right(result) => {
        cache.set(key, result)
        knownCacheKeys += key
        Right(result)
      }
      case error => error
    }
  }
  
  def removeAllCachedResults() {
    knownCacheKeys.foreach {
      key => cache.remove(key)
    }
    
    knownCacheKeys = Set()
  }
  
  def removeCached(key: String) {
    cache.remove(key)
    knownCacheKeys -= key
  }
  
  def removeCachedByPredicate(predicate: String => Boolean) {    
    val curKeys = knownCacheKeys
    
    curKeys.foreach {
      key => if (predicate(key))
        removeCached(key)
    }
  }
  
  def notSupported: Nothing = throw new UnsupportedOperationException("Operation not supported in cached implementation")
}