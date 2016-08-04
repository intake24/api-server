package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ResourceError
import uk.ac.ncl.openlab.intake24.AsServedSet

trait AsServedImageService {
   def asServedSet(id: String): Either[ResourceError, AsServedSet]
}