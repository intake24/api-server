package cache

import play.api.cache.CacheApi

trait ProblemCheckerCache {
  
  val cache: CacheApi
  
  def foodProblemsCacheKey(code: String, locale: String) = s"ProblemChecker.foodProblems.$locale.$code"
  def categoryProblemsCacheKey(code: String, locale: String) = s"ProblemChecker.categoryProblems.$locale.$code"
  def recursiveCategoryProblemsCacheKey(code: String, locale: String) = s"ProblemChecker.recursiveCategoryProblems.$locale.$code"
  def translationRequiredCacheKey(locale: String) = s"ProblemChecker.translationRequired.$locale"
  
  
  def invalidateFoodProblems(code: String, locale: String) = cache.remove(foodProblemsCacheKey(code, locale))
  def invalidateCategoryProblems(code: String, locale: String) = cache.remove(categoryProblemsCacheKey(code, locale))
  def invalidateRecursiveCategoryProblems(code: String, locale: String) = cache.remove(recursiveCategoryProblemsCacheKey(code, locale))
  def invalidateTranslationRequired(locale: String) = cache.remove(translationRequiredCacheKey(locale))
}