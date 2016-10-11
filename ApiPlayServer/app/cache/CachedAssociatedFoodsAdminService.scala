package cache

import com.google.inject.Inject

import modules.BasicImpl
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.AsServedHeader

import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleOrParentError

case class CachedAssociatedFoodsAdminService @Inject() (@BasicImpl service: AssociatedFoodsAdminService, cache: CacheApi)
    extends AssociatedFoodsAdminService
    with CacheResult {

  var knownCacheKeys = Set[String]()

  def associatedFoodsWithHeadersCacheKey(foodCode: String, locale: String) = s"CachedAssociatedFoodsAdminService.associatedFoodsWithHeaders.$locale.$foodCode"

  def getAssociatedFoods(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFood]] = service.getAssociatedFoods(foodCode, locale)
  
  def getAssociatedFoodsWithHeaders(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFoodWithHeader]] =
    cachePositiveResult(associatedFoodsWithHeadersCacheKey(foodCode, locale)) {
      service.getAssociatedFoodsWithHeaders(foodCode, locale)
    }

  def updateAssociatedFoods(foodCode: String, associatedFoods: Seq[AssociatedFood], locale: String): Either[LocaleOrParentError, Unit] =
    service.updateAssociatedFoods(foodCode, associatedFoods, locale).right.map {
      _ => cache.remove(associatedFoodsWithHeadersCacheKey(foodCode, locale))
    }

  def deleteAllAssociatedFoods(locale: String): Either[DatabaseError, Unit] = service.deleteAllAssociatedFoods(locale)

  def createAssociatedFoods(assocFoods: Map[String, Seq[AssociatedFood]], locale: String) = service.createAssociatedFoods(assocFoods, locale)
}
