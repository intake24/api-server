package cache

import com.google.inject.Inject


import play.api.cache.CacheApi
import uk.ac.ncl.openlab.intake24.AsServedHeader
import uk.ac.ncl.openlab.intake24.AsServedSetV1
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CreateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import modules.BasicImpl
import uk.ac.ncl.openlab.intake24.AsServedImageV1

case class CachedAsServedImageAdminService @Inject() (@BasicImpl service: AsServedImageAdminService, cache: CacheApi)
    extends AsServedImageAdminService
    with CacheResult {
  
  var knownCacheKeys = Set[String]()
  
  val listAsServedSetsCacheKey = "CachedAsServedImageAdminService.listAsServedSets"
  
  def asServedSetCacheKey(id: String) = s"CachedAsServedImageAdminService.asServedSet.$id"
  
  def listAsServedSets(): Either[DatabaseError, Map[String, AsServedHeader]] = cachePositiveResult(listAsServedSetsCacheKey) {
    service.listAsServedSets()
  }
  
  
  def createAsServedSets(sets: Seq[AsServedSetV1]): Either[CreateError, Unit] = service.createAsServedSets(sets)
  
  def deleteAllAsServedSets(): Either[DatabaseError, Unit] = notSupported
  
}