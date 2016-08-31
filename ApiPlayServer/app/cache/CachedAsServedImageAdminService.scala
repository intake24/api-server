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

case class CachedAsServedImageAdminService @Inject() (@UncachedImpl service: AsServedImageAdminService, cache: CacheApi)
    extends AsServedImageAdminService
    with CacheResult {
  
  var knownCacheKeys = Set[String]()
  
  val listAsServedSetsCacheKey = "CachedAsServedImageAdminService.listAsServedSets"
  
  def asServedSetCacheKey(id: String) = s"CachedAsServedImageAdminService.asServedSet.$id"
  
  def listAsServedSets(): Either[DatabaseError, Map[String, AsServedHeader]] = cachePositiveResult(listAsServedSetsCacheKey) {
    service.listAsServedSets()
  }
  
  def getAsServedSet(id: String): Either[LookupError, AsServedSet] = cachePositiveResult(asServedSetCacheKey(id)) {
    service.getAsServedSet(id)
  }
  
  def createAsServedSets(sets: Seq[AsServedSet]): Either[CreateError, Unit] = service.createAsServedSets(sets)
  
  def deleteAllAsServedSets(): Either[DatabaseError, Unit] = notSupported
  
}