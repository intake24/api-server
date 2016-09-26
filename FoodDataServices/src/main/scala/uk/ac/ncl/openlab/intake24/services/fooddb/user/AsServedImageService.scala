package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.AsServedSet

trait AsServedImageService {
  
   def getAsServedSet(id: String): Either[LookupError, AsServedSet]
}