package cache

import com.google.inject.Inject

import modules.UncachedImpl
import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError

case class CachedAssociatedFoodsAdminService @Inject() (@UncachedImpl service: AssociatedFoodsAdminService, cache: CacheApi)
    extends AsServedImageAdminService
    with CacheResult {

  def associatedFoodsWithHeadersCacheKey(foodCode: String, locale: String) = s"CachedAssociatedFoodsAdminService.associatedFoodsWithHeaders.$locale.$foodCode"

  def getAssociatedFoodsWithHeaders(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFoodWithHeader]] =
    cachePositiveResult(associatedFoodsWithHeadersCacheKey(foodCode, locale)) {
      service.getAssociatedFoodsWithHeaders(foodCode, locale)
    }

  def updateAssociatedFoods(foodCode: String, locale: String, associatedFoods: Seq[AssociatedFood]): Either[LocalUpdateError, Unit] =
    service.updateAssociatedFoods(foodCode, locale, associatedFoods).right.map {
      _ => cache.remove(associatedFoodsWithHeadersCacheKey(foodCode, locale))
    }

  def deleteAllAssociatedFoods(locale: String): Either[DatabaseError, Unit] = notSupported
  
  def createAssociatedFoods(assocFoods: Map[String, Seq[AssociatedFood]], locale: String) = notSupported
}
